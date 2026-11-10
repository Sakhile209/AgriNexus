import assert from 'node:assert/strict'
import test from 'node:test'
import {summarizeReports,periodStart,csvCell,reportCsv} from '../src/reports.js'
const now=new Date('2026-11-10T10:00:00Z')
test('reports filter dated records and distinguish registrations from current status',()=>{
 const data={animals:[{id:'1',species:'Cattle',status:'ACTIVE',createdAt:'2026-10-01T12:00:00Z'},{id:'2',species:'cattle',status:'SOLD',createdAt:'2026-08-01T12:00:00Z'}],health:[{createdAt:'2026-11-01T00:00:00Z'},{createdAt:'2026-06-01T00:00:00Z'}],vaccinations:[],crops:[]}
 const summary=summarizeReports(data,3,now)
 assert.equal(summary.total,2);assert.equal(summary.active,1);assert.equal(summary.inactive,1);assert.equal(summary.species.Cattle,2);assert.equal(summary.health.length,1)
 assert.deepEqual(summary.trend.map(p=>p.count),[1,2,2]);assert.equal(periodStart(3,now),'2026-09-01')
 assert.equal(summarizeReports(data,6,now).health.length,2)
})
test('a newer vaccination replaces an older due schedule for the same animal and vaccine',()=>{
 const vaccinations=[{animalId:'1',vaccineName:'FMD',administeredOn:'2026-06-01',nextDueOn:'2026-07-01'},{animalId:'1',vaccineName:'fmd',administeredOn:'2026-11-01',nextDueOn:'2027-01-01'}]
 const summary=summarizeReports({vaccinations},3,now)
 assert.deepEqual(summary.schedules,{Completed:1,Upcoming:0,Overdue:0});assert.equal(summary.vaccinations.length,1)
})
test('empty records stay empty and CSV export neutralizes spreadsheet formula injection',()=>{
 const summary=summarizeReports({},3,now);assert.equal(summary.total,0);assert.equal(summary.health.length,0);assert.deepEqual(summary.species,{})
 assert.equal(csvCell('=HYPERLINK("bad")'),'"\'=HYPERLINK(""bad"")"');assert.equal(csvCell('Cow, "C001"'),'"Cow, ""C001"""')
})

test('CSV contains only the selected report category',()=>{
 const data={animals:[{internalId:'C001',species:'Cattle'}],health:[],vaccinations:[],crops:[{cropType:'Maize',fieldName:'North',status:'GROWING',plantingDate:'2026-11-01'}]}
 const summary=summarizeReports(data,3,now)
 const csv=reportCsv(data,summary,{name:'Test farm'},'Crops')
 assert.match(csv,/Maize/);assert.doesNotMatch(csv,/C001/)
 const finance=reportCsv(data,summary,{name:'Test farm'},'Financial');assert.match(finance,/Not available/);assert.doesNotMatch(finance,/Maize/)
})
