const SOUTH_AFRICAN_PROVINCES = [
  'Eastern Cape',
  'Free State',
  'Gauteng',
  'KwaZulu-Natal',
  'Limpopo',
  'Mpumalanga',
  'North West',
  'Northern Cape',
  'Western Cape',
]

const MUNICIPALITIES_BY_PROVINCE = {
  'Eastern Cape': `Alfred Nzo District Municipality|Amahlathi Local Municipality|Amathole District Municipality|Blue Crane Route Local Municipality|Buffalo City Metropolitan Municipality|Chris Hani District Municipality|Dr AB Xuma Local Municipality|Dr Beyers Naudé Local Municipality|Elundini Local Municipality|Emalahleni Local Municipality|Enoch Mgijima Local Municipality|Great Kei Local Municipality|Ingquza Hill Local Municipality|Intsika Yethu Local Municipality|Inxuba Yethemba Local Municipality|Joe Gqabi District Municipality|King Sabata Dalindyebo Local Municipality|Kouga Local Municipality|KouKamma Local Municipality|Makana Local Municipality|Matatiele Local Municipality|Mbhashe Local Municipality|Mhlontlo Local Municipality|Mnquma Local Municipality|Ndlambe Local Municipality|Nelson Mandela Bay Metropolitan Municipality|Ngqushwa Local Municipality|Ntabankulu Local Municipality|Nyandeni Local Municipality|OR Tambo District Municipality|Port St Johns Local Municipality|Raymond Mhlaba Local Municipality|Sakhisizwe Local Municipality|Sarah Baartman District Municipality|Senqu Local Municipality|Sunday's River Valley Local Municipality|Umzimvubu Local Municipality|Walter Sisulu Local Municipality|Winnie Madikizela-Mandela Local Municipality`.split('|'),
  'Free State': `Dihlabeng Local Municipality|Fezile Dabi District Municipality|Kopanong Local Municipality|Lejweleputswa District Municipality|Letsemeng Local Municipality|Mafube Local Municipality|Maluti-a-Phofung Local Municipality|Mangaung Metropolitan Municipality|Mantsopa Local Municipality|Masilonyana Local Municipality|Matjhabeng Local Municipality|Metsimaholo Local Municipality|Mohokare Local Municipality|Moqhaka Local Municipality|Nala Local Municipality|Ngwathe Local Municipality|Nketoana Local Municipality|Phumelela Local Municipality|Setsoto Local Municipality|Thabo Mofutsanyana District Municipality|Tokologo Local Municipality|Tswelopele Local Municipality|Xhariep District Municipality`.split('|'),
  Gauteng: `City of Ekurhuleni|City of Johannesburg Metropolitan Municipality|City of Tshwane Metropolitan Municipality|Emfuleni Local Municipality|Lesedi Local Municipality|Merafong Local Municipality|Midvaal Local Municipality|Mogale City Local Municipality|Rand West City Local Municipality|Sedibeng District Municipality|West Rand District Municipality`.split('|'),
  'KwaZulu-Natal': `AbaQulusi Local Municipality|Alfred Duma Local Municipality|Amajuba District Municipality|Big 5 Hlabisa Local Municipality|City of uMhlathuze Local Municipality|Dannhauser Local Municipality|Dr Nkosazana Dlamini Zuma Local Municipality|eDumbe Local Municipality|Emadlangeni Local Municipality|Endumeni Local Municipality|eThekwini Metropolitan Municipality|Greater Kokstad Local Municipality|Harry Gwala District Municipality|iLembe District Municipality|Impendle Local Municipality|Inkosi Langalibalele Local Municipality|Inkosi Mtubatuba Local Municipality|Johannes Phumani Phungula Local Municipality|Jozini Local Municipality|King Cetshwayo District Municipality|KwaDukuza Local Municipality|Mandeni Local Municipality|Maphumulo Local Municipality|Mkhambathini Local Municipality|Mpofana Local Municipality|Msunduzi Local Municipality|Mthonjaneni Local Municipality|Ndwedwe Local Municipality|Newcastle Local Municipality|Nkandla Local Municipality|Nongoma Local Municipality|Nquthu Local Municipality|Okhahlamba Local Municipality|Ray Nkonyeni Local Municipality|Richmond Local Municipality|Ugu District Municipality|Ulundi Local Municipality|Umdoni Local Municipality|uMfolozi Local Municipality|uMgungundlovu District Municipality|Umhlabuyalingana Local Municipality|uMkhanyakude District Municipality|uMlalazi Local Municipality|uMngeni Local Municipality|uMshwathi Local Municipality|uMsinga Local Municipality|Umuziwabantu Local Municipality|Umvoti Local Municipality|Umzimkhulu Local Municipality|uMzinyathi District Municipality|Umzumbe Local Municipality|uPhongolo Local Municipality|uThukela District Municipality|Zululand District Municipality`.split('|'),
  Limpopo: `Ba-Phalaborwa Local Municipality|Bela-Bela Local Municipality|Blouberg Local Municipality|Capricorn District Municipality|Elias Motswaledi Local Municipality|Ephraim Mogale Local Municipality|Fetakgomo Tubatse Local Municipality|Greater Giyani Local Municipality|Greater Letaba Local Municipality|Greater Tzaneen Local Municipality|Lepelle-Nkumpi Local Municipality|Lephalale Local Municipality|Makhado Local Municipality|Makhudutamaga Local Municipality|Maruleng Local Municipality|Modimolle-Mookgophong Local Municipality|Mogalakwena Local Municipality|Molemole Local Municipality|Mopani District Municipality|Musina Local Municipality|Polokwane Local Municipality|Sekhukhune District Municipality|Thabazimbi Local Municipality|Thulamela Local Municipality|Vhembe District Municipality|Waterberg District Municipality`.split('|'),
  Mpumalanga: `Bushbuckridge Local Municipality|Chief Albert Luthuli Local Municipality|City of Mbombela Local Municipality|Dipaleseng Local Municipality|Dr JS Moroka Local Municipality|Ehlanzeni District Municipality|Emakhazeni Local Municipality|Emalahleni Local Municipality|Gert Sibande District Municipality|Govan Mbeki Local Municipality|Lekwa Local Municipality|Mkhondo Local Municipality|Msukaligwa Local Municipality|Nkangala District Municipality|Nkomazi Local Municipality|Pixley Ka Seme Local Municipality|Steve Tshwete Local Municipality|Thaba Chweu Local Municipality|Thembisile Hani Local Municipality|Victor Khanye Local Municipality`.split('|'),
  'North West': `Bojanala Platinum District Municipality|City of Matlosana Local Municipality|Ditsobotla Local Municipality|Dr Kenneth Kaunda District Municipality|Dr Ruth Segomotsi Mompati District Municipality|Greater Taung Local Municipality|Kagisano-Molopo Local Municipality|Kgetlengrivier Local Municipality|Lekwa-Teemane Local Municipality|Madibeng Local Municipality|Mahikeng Local Municipality|Mamusa Local Municipality|Maquassi Hills Local Municipality|Moretele Local Municipality|Moses Kotane Local Municipality|Naledi Local Municipality|Ngaka Modiri Molema District Municipality|Ramotshere Moiloa Local Municipality|Ratlou Local Municipality|Rustenburg Local Municipality|Tswaing Local Municipality`.split('|'),
  'Northern Cape': `!Kheis Local Municipality|Dawid Kruiper Local Municipality|Dikgatlong Local Municipality|Emthanjeni Local Municipality|Frances Baard District Municipality|Ga-segonyana Local Municipality|Gamagara Local Municipality|Hantam Local Municipality|Joe Morolong Local Municipality|John Taolo Gaetsewe District Municipality|Kai !Garib Local Municipality|Kamiesberg Local Municipality|Kareeberg Local Municipality|Karoo Hoogland Local Municipality|Kgatelopele Local Municipality|Khâi-ma Local Municipality|Magareng Local Municipality|Nama Khoi Local Municipality|Namakwa District Municipality|Phokwane Local Municipality|Pixley Ka Seme District Municipality|Renosterberg Local Municipality|Richtersveld Local Municipality|Siyancuma Local Municipality|Siyathemba Local Municipality|Sol Plaatje Local Municipality|Thembelihle Local Municipality|Tsantsabane Local Municipality|Ubuntu Local Municipality|Umsobomvu Local Municipality|ZF Mgcawu District Municipality`.split('|'),
  'Western Cape': `Beaufort West Local Municipality|Bergrivier Local Municipality|Bitou Local Municipality|Breede Valley Local Municipality|Cape Agulhas Local Municipality|Cape Winelands District Municipality|Cederberg Local Municipality|Central Karoo District Municipality|City of Cape Town Metropolitan Municipality|Drakenstein Local Municipality|Garden Route District Municipality|George Local Municipality|Hessequa Local Municipality|Kannaland Local Municipality|Knysna Local Municipality|Laingsburg Local Municipality|Langeberg Local Municipality|Matzikama Local Municipality|Mossel Bay Local Municipality|Oudtshoorn Local Municipality|Overberg District Municipality|Overstrand Local Municipality|Prince Albert Local Municipality|Saldanha Bay Local Municipality|Stellenbosch Local Municipality|Swartland Local Municipality|Swellendam Local Municipality|Theewaterskloof Local Municipality|West Coast District Municipality|Witzenberg Local Municipality`.split('|'),
}

function enhanceProvinceField() {
  const provinceInput = document.querySelector('input[name="province"]')
  if (!provinceInput) return

  const provinceSelect = document.createElement('select')
  provinceSelect.name = 'province'
  provinceSelect.required = true
  provinceSelect.setAttribute('aria-label', 'Province')

  const prompt = document.createElement('option')
  prompt.value = ''
  prompt.textContent = 'Select a province'
  prompt.disabled = true
  prompt.selected = true
  provinceSelect.append(prompt)

  SOUTH_AFRICAN_PROVINCES.forEach((province) => {
    const option = document.createElement('option')
    option.value = province
    option.textContent = province
    provinceSelect.append(option)
  })

  provinceInput.replaceWith(provinceSelect)
}

function setMunicipalityOptions(provinceSelect, municipalitySelect) {
  municipalitySelect.replaceChildren()

  const prompt = document.createElement('option')
  prompt.value = ''
  prompt.textContent = provinceSelect.value
    ? 'Select a municipality or region'
    : 'Select a province first'
  prompt.disabled = true
  prompt.selected = true
  municipalitySelect.append(prompt)
  municipalitySelect.disabled = !provinceSelect.value

  ;(MUNICIPALITIES_BY_PROVINCE[provinceSelect.value] || []).forEach((municipality) => {
    const option = document.createElement('option')
    option.value = municipality
    option.textContent = municipality
    municipalitySelect.append(option)
  })
}

function enhanceMunicipalityField() {
  const provinceSelect = document.querySelector('select[name="province"]')
  const municipalityInput = document.querySelector('input[name="municipality"]')
  if (!provinceSelect || !municipalityInput) return

  const municipalitySelect = document.createElement('select')
  municipalitySelect.name = 'municipality'
  municipalitySelect.required = true
  municipalitySelect.setAttribute('aria-label', 'Municipality or region')
  municipalityInput.replaceWith(municipalitySelect)

  setMunicipalityOptions(provinceSelect, municipalitySelect)
  provinceSelect.addEventListener('change', () => {
    setMunicipalityOptions(provinceSelect, municipalitySelect)
  })
}

enhanceProvinceField()
enhanceMunicipalityField()
new MutationObserver(() => {
  enhanceProvinceField()
  enhanceMunicipalityField()
}).observe(document.body, {
  childList: true,
  subtree: true,
})
