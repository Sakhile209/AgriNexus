import { StatusBar } from 'expo-status-bar'
import * as SecureStore from 'expo-secure-store'
import { useEffect, useState } from 'react'
import { ActivityIndicator, Alert, FlatList, KeyboardAvoidingView, Platform, Pressable, RefreshControl, SafeAreaView, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native'

const API_URL = process.env.EXPO_PUBLIC_API_URL ?? (Platform.OS === 'android' ? 'http://10.0.2.2:8080/api/v1' : 'http://localhost:8080/api/v1')
const SESSION_KEY = 'agrinexus.session'
type Json = Record<string, any>
type Session = { accessToken: string; user: Json }
type Farm = { id: string; name: string; farmType: string; province: string }
type Tab = 'Home'|'Livestock'|'Crops'|'Market'|'Alerts'

async function request(path:string, options:RequestInit={}, token?:string) {
  const response = await fetch(`${API_URL}${path}`, { ...options, headers:{'Content-Type':'application/json', ...(token?{Authorization:`Bearer ${token}`}:{}) ,...options.headers} })
  const body = await response.json().catch(()=>({}))
  if (!response.ok) throw new Error(body.message || `Request failed (${response.status})`)
  return body
}

function Field({label,value,onChangeText,secure=false,keyboardType='default'}:{label:string;value:string;onChangeText:(v:string)=>void;secure?:boolean;keyboardType?:any}) {
  return <View style={s.field}><Text style={s.label}>{label}</Text><TextInput style={s.input} value={value} onChangeText={onChangeText} secureTextEntry={secure} keyboardType={keyboardType} autoCapitalize={keyboardType==='email-address'?'none':'sentences'}/></View>
}

function Button({title,onPress,secondary=false}:{title:string;onPress:()=>void;secondary?:boolean}) { return <Pressable style={[s.button,secondary&&s.buttonSecondary]} onPress={onPress}><Text style={[s.buttonText,secondary&&s.buttonTextSecondary]}>{title}</Text></Pressable> }

function Auth({done}:{done:(x:Session)=>void}) {
  const [register,setRegister]=useState(false),[busy,setBusy]=useState(false)
  const [form,setForm]=useState({firstName:'',lastName:'',phoneNumber:'',email:'',password:''})
  const set=(key:string)=>(value:string)=>setForm({...form,[key]:value})
  const submit=async()=>{setBusy(true);try{const result=await request(`/auth/${register?'register':'login'}`,{method:'POST',body:JSON.stringify(form)});await SecureStore.setItemAsync(SESSION_KEY,JSON.stringify(result));done(result)}catch(e){Alert.alert('Unable to continue',(e as Error).message)}finally{setBusy(false)}}
  return <KeyboardAvoidingView style={s.auth} behavior={Platform.OS==='ios'?'padding':undefined}><ScrollView contentContainerStyle={s.authBody}><View style={s.logo}><Text style={s.logoIcon}>🌱</Text></View><Text style={s.brand}>Agri<Text style={s.green}>Nexus</Text></Text><Text style={s.authTitle}>{register?'Create your account':'Welcome back'}</Text><Text style={s.muted}>{register?'Start managing your farm from your phone.':'Sign in to continue managing your farm.'}</Text>{register&&<><Field label="First name" value={form.firstName} onChangeText={set('firstName')}/><Field label="Last name" value={form.lastName} onChangeText={set('lastName')}/><Field label="Phone number" value={form.phoneNumber} onChangeText={set('phoneNumber')} keyboardType="phone-pad"/></>}<Field label="Email" value={form.email} onChangeText={set('email')} keyboardType="email-address"/><Field label="Password" value={form.password} onChangeText={set('password')} secure/>{busy?<ActivityIndicator color="#159653"/>:<Button title={register?'Create account':'Log in'} onPress={submit}/>}<Pressable onPress={()=>setRegister(!register)}><Text style={s.authSwitch}>{register?'Already registered? Log in':'New to AgriNexus? Create an account'}</Text></Pressable></ScrollView></KeyboardAvoidingView>
}

function FarmSetup({token,done}:{token:string;done:()=>void}) {
  const [f,setF]=useState({name:'',farmType:'',province:'',municipality:'',latitude:'',longitude:''})
  const set=(key:string)=>(value:string)=>setF({...f,[key]:value})
  const submit=async()=>{try{await request('/farms',{method:'POST',body:JSON.stringify(f)},token);done()}catch(e){Alert.alert('Farm not saved',(e as Error).message)}}
  return <ScrollView contentContainerStyle={s.page}><Text style={s.eyebrow}>FIRST STEP</Text><Text style={s.title}>Register your farm</Text><Text style={s.muted}>Coordinates enable local weather information.</Text><Field label="Farm name" value={f.name} onChangeText={set('name')}/><Field label="Farm type" value={f.farmType} onChangeText={set('farmType')}/><Field label="Province" value={f.province} onChangeText={set('province')}/><Field label="Municipality" value={f.municipality} onChangeText={set('municipality')}/><Field label="Latitude" value={f.latitude} onChangeText={set('latitude')} keyboardType="numeric"/><Field label="Longitude" value={f.longitude} onChangeText={set('longitude')} keyboardType="numeric"/><Button title="Create farm" onPress={submit}/></ScrollView>
}

function Card({children}:{children:any}) { return <View style={s.card}>{children}</View> }
function Metric({value,label}:{value:any;label:string}) { return <Card><Text style={s.metric}>{value}</Text><Text style={s.mutedSmall}>{label}</Text></Card> }

function Home({token,farm}:{token:string;farm:Farm}) {
  const [data,setData]=useState<Json|null>(null),[refreshing,setRefreshing]=useState(false)
  const load=async()=>{try{setData(await request(`/farms/${farm.id}/dashboard`,{},token))}catch(e){Alert.alert('Dashboard unavailable',(e as Error).message)}}
  useEffect(()=>{load()},[farm.id]); const refresh=async()=>{setRefreshing(true);await load();setRefreshing(false)}
  if(!data)return <ActivityIndicator style={s.loader} color="#159653"/>
  const w=data.weather
  return <ScrollView contentContainerStyle={s.page} refreshControl={<RefreshControl refreshing={refreshing} onRefresh={refresh}/>}><Text style={s.eyebrow}>FARM OVERVIEW</Text><Text style={s.title}>{farm.name}</Text><View style={s.metrics}><Metric value={data.activeLivestock} label="Livestock"/><Metric value={data.activeCrops} label="Active crops"/><Metric value={data.unreadNotifications} label="Alerts"/><Metric value={data.upcomingVaccinations} label="Due soon"/></View><View style={s.weather}><Text style={s.weatherLabel}>LOCAL WEATHER</Text><Text style={s.weatherValue}>{w.available?`${Math.round(w.temperatureC)}°C`:'—'}</Text><Text style={s.weatherCopy}>{w.available?`${w.rainProbability??0}% rain · ${w.windSpeedKph??0} km/h wind`:w.message}</Text></View><Text style={s.sectionTitle}>Livestock summary</Text><Card>{Object.keys(data.livestockBySpecies).length?Object.entries(data.livestockBySpecies).map(([k,v])=><View style={s.row} key={k}><Text>{k}</Text><Text style={s.rowStrong}>{String(v)}</Text></View>):<Text style={s.muted}>No livestock registered yet.</Text>}</Card></ScrollView>
}

function ResourceList({title,path,token,farm,kind}:{title:string;path:string;token:string;farm:Farm;kind:'animal'|'field'|'notice'}) {
  const [items,setItems]=useState<Json[]>([]),[loading,setLoading]=useState(true)
  const load=async()=>{try{setItems(await request(path.replace(':farmId',farm.id),{},token))}catch(e){Alert.alert(`${title} unavailable`,(e as Error).message)}finally{setLoading(false)}}
  useEffect(()=>{load()},[farm.id]); if(loading)return <ActivityIndicator style={s.loader} color="#159653"/>
  return <FlatList contentContainerStyle={s.page} data={items} keyExtractor={x=>x.id} ListHeaderComponent={<><Text style={s.eyebrow}>{farm.name.toUpperCase()}</Text><Text style={s.title}>{title}</Text></>} ListEmptyComponent={<Card><Text style={s.muted}>No records yet. Use the web dashboard to add your first record.</Text></Card>} renderItem={({item})=><Card><Text style={s.itemTitle}>{kind==='animal'?item.internalId:kind==='field'?item.name:item.title}</Text><Text style={s.muted}>{kind==='animal'?`${item.species} · ${item.breed||'Breed not recorded'}`:kind==='field'?`${item.sizeValue||'—'} ${item.sizeUnit||''} · ${item.soilType||'Soil unknown'}`:item.message}</Text>{kind==='notice'&&!item.read&&<Button title="Mark as read" onPress={async()=>{await request(`/notifications/${item.id}/read`,{method:'PATCH'},token);load()}}/>}</Card>}/>
}

function Market({token}:{token:string}) {
  const [prices,setPrices]=useState<Json[]>([]),[listings,setListings]=useState<Json[]>([])
  useEffect(()=>{Promise.all([request('/market-prices',{},token),request('/marketplace/listings',{},token)]).then(([p,l])=>{setPrices(p);setListings(l)}).catch(e=>Alert.alert('Market unavailable',e.message))},[])
  return <ScrollView contentContainerStyle={s.page}><Text style={s.eyebrow}>LIVE AGRICULTURAL MARKET</Text><Text style={s.title}>Prices</Text>{prices.map(x=><Card key={x.id}><View style={s.row}><View><Text style={s.itemTitle}>{x.commodity}</Text><Text style={s.mutedSmall}>{x.market} · {x.province}</Text></View><Text style={s.price}>R {x.priceZar}/{x.unit}</Text></View></Card>)}<Text style={s.sectionTitle}>Marketplace</Text>{listings.map(x=><Card key={x.id}><Text style={s.itemTitle}>{x.title}</Text><Text style={s.muted}>{x.category} · {x.location}</Text><Text style={s.price}>R {x.priceZar} / {x.unit}</Text><Text style={s.mutedSmall}>{x.quantity} available · {x.contact}</Text></Card>)}</ScrollView>
}

function AppShell({session,logout}:{session:Session;logout:()=>void}) {
  const [farms,setFarms]=useState<Farm[]>([]),[farm,setFarm]=useState<Farm|null>(null),[tab,setTab]=useState<Tab>('Home'),[loading,setLoading]=useState(true)
  const load=async()=>{try{const all=await request('/farms',{},session.accessToken);setFarms(all);setFarm(current=>current??all[0]??null)}catch(e){Alert.alert('Unable to load farms',(e as Error).message)}finally{setLoading(false)}}
  useEffect(()=>{load()},[]);if(loading)return <ActivityIndicator style={s.loader} color="#159653"/>;if(!farm)return <FarmSetup token={session.accessToken} done={load}/>
  const content=tab==='Home'?<Home token={session.accessToken} farm={farm}/>:tab==='Livestock'?<ResourceList title="Livestock" path="/farms/:farmId/animals" token={session.accessToken} farm={farm} kind="animal"/>:tab==='Crops'?<ResourceList title="Fields & crops" path="/farms/:farmId/fields" token={session.accessToken} farm={farm} kind="field"/>:tab==='Market'?<Market token={session.accessToken}/>:<ResourceList title="Alerts" path="/notifications" token={session.accessToken} farm={farm} kind="notice"/>
  return <SafeAreaView style={s.shell}><View style={s.topbar}><View><Text style={s.brandSmall}>Agri<Text style={s.green}>Nexus</Text></Text><Text style={s.mutedSmall}>{farm.name}</Text></View><Pressable onPress={logout}><Text style={s.logout}>Log out</Text></Pressable></View><View style={s.content}>{content}</View><View style={s.tabs}>{(['Home','Livestock','Crops','Market','Alerts'] as Tab[]).map(x=><Pressable key={x} style={s.tab} onPress={()=>setTab(x)}><Text style={[s.tabIcon,tab===x&&s.green]}>{x==='Home'?'⌂':x==='Livestock'?'♧':x==='Crops'?'🌱':x==='Market'?'↗':'◇'}</Text><Text style={[s.tabText,tab===x&&s.green]}>{x}</Text></Pressable>)}</View></SafeAreaView>
}

export default function App(){const[session,setSession]=useState<Session|null>(null),[ready,setReady]=useState(false);useEffect(()=>{SecureStore.getItemAsync(SESSION_KEY).then(value=>{if(value)setSession(JSON.parse(value))}).finally(()=>setReady(true))},[]);const logout=async()=>{await SecureStore.deleteItemAsync(SESSION_KEY);setSession(null)};if(!ready)return <ActivityIndicator style={s.loader} color="#159653"/>;return <><StatusBar style="dark"/>{session?<AppShell session={session} logout={logout}/>:<Auth done={setSession}/>}</>}

const s=StyleSheet.create({shell:{flex:1,backgroundColor:'#f5f8f5'},content:{flex:1},loader:{flex:1},topbar:{height:68,paddingHorizontal:20,flexDirection:'row',alignItems:'center',justifyContent:'space-between',borderBottomWidth:1,borderColor:'#e2e8e3',backgroundColor:'#fff'},brandSmall:{fontSize:20,fontWeight:'800'},logout:{color:'#137b43',fontWeight:'700'},tabs:{height:72,flexDirection:'row',borderTopWidth:1,borderColor:'#dde5df',backgroundColor:'#fff'},tab:{flex:1,alignItems:'center',justifyContent:'center',gap:3},tabIcon:{fontSize:18,color:'#7b8980'},tabText:{fontSize:10,color:'#66746b'},page:{padding:20,paddingBottom:36,gap:12},auth:{flex:1,backgroundColor:'#eff8f1'},authBody:{padding:28,paddingTop:80},logo:{width:54,height:54,borderRadius:15,alignItems:'center',justifyContent:'center',backgroundColor:'#159653'},logoIcon:{fontSize:27},brand:{fontSize:28,fontWeight:'900',marginTop:12},green:{color:'#159653'},authTitle:{fontSize:28,fontWeight:'800',marginTop:42,marginBottom:6},authSwitch:{marginTop:22,textAlign:'center',color:'#137b43',fontWeight:'700'},eyebrow:{fontSize:11,fontWeight:'800',letterSpacing:1.2,color:'#19844b'},title:{fontSize:28,fontWeight:'800',color:'#17251d',marginBottom:4},sectionTitle:{fontSize:18,fontWeight:'800',marginTop:12},muted:{color:'#6d7b72',lineHeight:20},mutedSmall:{fontSize:11,color:'#7b8880'},field:{gap:6,marginTop:12},label:{fontSize:12,fontWeight:'700',color:'#37463d'},input:{height:50,borderWidth:1,borderColor:'#d8e1da',borderRadius:12,paddingHorizontal:14,backgroundColor:'#fff'},button:{minHeight:48,marginTop:14,borderRadius:12,alignItems:'center',justifyContent:'center',backgroundColor:'#159653'},buttonSecondary:{backgroundColor:'#e8f5ed'},buttonText:{color:'#fff',fontWeight:'800'},buttonTextSecondary:{color:'#137b43'},card:{padding:16,borderRadius:16,borderWidth:1,borderColor:'#e0e7e2',backgroundColor:'#fff',marginBottom:4},metrics:{flexDirection:'row',flexWrap:'wrap',gap:8},metric:{fontSize:24,fontWeight:'800',color:'#163d27'},weather:{padding:22,borderRadius:20,backgroundColor:'#0c6540'},weatherLabel:{fontSize:10,fontWeight:'800',letterSpacing:1,color:'#bde3cc'},weatherValue:{fontSize:48,fontWeight:'700',color:'#fff',marginVertical:8},weatherCopy:{color:'#dbefe3'},row:{flexDirection:'row',justifyContent:'space-between',alignItems:'center',gap:12},rowStrong:{fontWeight:'800'},itemTitle:{fontSize:16,fontWeight:'800',marginBottom:5},price:{color:'#148147',fontWeight:'900',fontSize:15}})
