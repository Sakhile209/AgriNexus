"""Smoke tests against a running local API. Creates uniquely named test accounts and cleans up only those accounts via the existing database container."""
import base64,json,os,struct,subprocess,time,urllib.request,urllib.error,uuid,zlib
base=os.environ.get('AGRINEXUS_API_URL','http://localhost:5174/api/v1')
run=uuid.uuid4().hex
emails=[f'live-test-{run}-{n}@example.invalid' for n in (1,2)]
checks=[]
def call(path,method='GET',body=None,token=None,content_type='application/json'):
 headers={'Content-Type':content_type}
 if token:headers['Authorization']='Bearer '+token
 raw=(json.dumps(body).encode() if isinstance(body,(dict,list)) else body) if body is not None else None
 req=urllib.request.Request(base+path,data=raw,headers=headers,method=method)
 try:
  with urllib.request.urlopen(req,timeout=30) as r:data=r.read();return r.status,json.loads(data) if data and 'json' in r.headers.get('Content-Type','') else data
 except urllib.error.HTTPError as e:
  data=e.read();return e.code,json.loads(data) if data and 'json' in e.headers.get('Content-Type','') else data
try:
 tokens=[]
 for email in emails:
  status,result=call('/auth/register','POST',{'firstName':'Integration','lastName':'Test','email':email,'phoneNumber':'+27000000000','password':uuid.uuid4().hex+'!aA1'})
  assert status==201,(status,result);tokens.append(result['accessToken'])
 owner,other=tokens
 farm={'name':'Integration test farm '+run,'farmType':'Mixed','province':'KwaZulu-Natal','municipality':'Newcastle','latitude':None,'longitude':None}
 status,result=call('/farms','POST',farm,owner);assert status==201,(status,result);farm_id=result['id']
 assert call('/farms/'+farm_id+'/weather')[0]==401
 assert call('/farms/'+farm_id+'/weather',token=other)[0]==404
 assert call('/farms/'+farm_id+'/map-config',token=other)[0]==404
 checks.append('farm weather/map authorization')
 status,w=call('/farms/'+farm_id+'/weather',token=owner);assert status==200 and w['status']=='missing_location' and w['retrievedAt'] is None
 checks.append('missing coordinates never use municipality')
 invalid={**farm,'latitude':91,'longitude':29.93};assert call('/farms','POST',invalid,owner)[0]==400
 invalid={**farm,'latitude':-27.75,'longitude':None};assert call('/farms','POST',invalid,owner)[0]==400
 checks.append('coordinate range/pair validation')
 farm.update(latitude=-27.75,longitude=29.93)
 assert call('/farms/'+farm_id,'PATCH',farm,owner)[0]==200
 status,w=call('/farms/'+farm_id+'/weather',token=owner);assert status==200
 weather_state={'available':w['available'],'status':w['status'],'provider':w['provider']}
 if w['available']:
  assert w['retrievedAt'] and w['observedAt'] and len(w['forecast'])==7
  _,again=call('/farms/'+farm_id+'/weather',token=owner);assert again['retrievedAt']==w['retrievedAt'];checks.append('live weather and cache reuse')
 else:assert w['retrievedAt'] is None;checks.append('provider-unavailable state without fabricated weather')
 offer={'title':'Integration test maize '+run,'category':'Grain','quantity':3,'unit':'kg','priceZar':8,'location':'Test pickup only','contact':emails[0],'province':'KwaZulu-Natal'}
 status,p=call('/marketplace/listings','POST',offer,owner);assert status==201,(status,p);listing_id=p['id']
 assert call('/marketplace/listings/'+listing_id,'PATCH',{'listing':offer,'active':False},other)[0]==404
 assert call('/marketplace/listings/'+listing_id,'PATCH',{'listing':offer,'active':False})[0]==401
 checks.append('listing owner-only edits')
 # A generated one-pixel PNG fixture, never a production product photo.
 def chunk(kind,data):return struct.pack('!I',len(data))+kind+data+struct.pack('!I',zlib.crc32(kind+data)&0xffffffff)
 png=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('!2I5B',1,1,8,2,0,0,0))+chunk(b'IDAT',zlib.compress(b'\x00\x00\xff\x00'))+chunk(b'IEND',b'')
 boundary='agrinexus'+run
 upload=f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="fixture.png"\r\nContent-Type: image/png\r\n\r\n'.encode()+png+f'\r\n--{boundary}--\r\n'.encode()
 assert call('/marketplace/listings/'+listing_id+'/photo','POST',upload,other,'multipart/form-data; boundary='+boundary)[0]==404
 status,result=call('/marketplace/listings/'+listing_id+'/photo','POST',upload,owner,'multipart/form-data; boundary='+boundary);assert status==200,(status,result)
 assert call('/marketplace/listings/'+listing_id+'/photo',token=owner)[0]==200
 checks.append('photo upload and authorization')
 assert call('/marketplace/listings/'+listing_id,'PATCH',{'listing':offer,'active':False},owner)[0]==200
 _,active=call('/marketplace/listings?province=KwaZulu-Natal');assert all(p['id']!=listing_id for p in active)
 _,mine=call('/marketplace/mine',token=owner);assert any(p['id']==listing_id and not p['active'] for p in mine)
 checks.append('availability and province-filtered retrieval')
 _,feed=call('/market-prices/status',token=owner);assert all(p.get('publishedOn') and p.get('sourceUrl') for p in feed['prices'])
 assert all(p['source']!='AgriNexus verified contributor' for p in feed['prices']);checks.append('seed exclusion and source metadata')
 print(json.dumps({'passed':checks,'weather':weather_state}))
finally:
 # Emails contain only a fixed prefix, a generated hex UUID, and a test-domain suffix.
 for email in emails:
  assert email.startswith('live-test-'+run+'-') and email.endswith('@example.invalid')
 values=','.join("'"+email+"'" for email in emails)
 sql=f"BEGIN; DELETE FROM marketplace_listing WHERE seller_email IN ({values}); DELETE FROM farm WHERE owner_id IN (SELECT id FROM app_user WHERE email IN ({values})); DELETE FROM farmer_profile WHERE user_id IN (SELECT id FROM app_user WHERE email IN ({values})); DELETE FROM app_user WHERE email IN ({values}); COMMIT;"
 subprocess.run(['docker','exec','-i','agrinexus-database-1','psql','-U','agrinexus','-d','agrinexus','-v','ON_ERROR_STOP=1'],input=sql.encode(),stdout=subprocess.DEVNULL,check=True)
