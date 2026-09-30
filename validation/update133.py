import subprocess,json,time,pathlib
pkg='com.bakrahisab.app'
def adb(*args):return subprocess.check_output(['adb',*args],text=True).strip()
assert 'Success' in adb('install','bakra-hisab-dist/Bakra-Hisab-1.3.2.apk')
adb('root');adb('wait-for-device')
adb('shell','am','start','-W','-n',pkg+'/.MainActivity');time.sleep(5);adb('shell','am','force-stop',pkg)
fixture=subprocess.check_output(['node','-e',"const E=require('./bakra-hisab/app/src/main/assets/engine.js');const d=E.newDB();d.current='test';d.seasons=[{id:'test',name:'Upgrade test',start:'2026-01-01',opening:100000}];d.goats=[{id:'goat',season:'test',tag:'Test-1',date:'2026-01-01',cost:2700000,paidByMe:2700000}];d.expenses=[{id:'food',season:'test',date:'2026-01-02',category:'Feeding',total:800000,share:800000,paidByMe:800000}];d.employees=[{id:'worker',season:'test',name:'Test worker',amount:200000,percent:100}];E.validate(d);console.log(JSON.stringify(d))"],text=True)
pathlib.Path('fixture.json').write_text(fixture)
adb('push','fixture.json','/data/local/tmp/hisab-fixture.json')
base='/data/data/'+pkg;uid=adb('shell','stat','-c','%u',base);assert uid.isdigit()
path=base+'/files/hisab-v1.json'
adb('shell','mkdir','-p',base+'/files');adb('shell','cp','/data/local/tmp/hisab-fixture.json',path);adb('shell','chown',uid+':'+uid,path);adb('shell','chmod','600',path);adb('shell','restorecon','-R',base+'/files')
def launch_check():
 adb('logcat','-c');adb('shell','am','start','-W','-n',pkg+'/.MainActivity');time.sleep(6)
 assert adb('shell','pidof',pkg),'App did not stay running'
 actual=json.loads(adb('shell','cat',path));expected=json.loads(fixture)
 for key in ['seasons','goats','expenses','employees','current']:assert actual[key]==expected[key],key+' changed'
 assert 'FATAL EXCEPTION' not in adb('logcat','-d','-s','AndroidRuntime')
launch_check();adb('shell','am','force-stop',pkg)
assert 'Success' in adb('install','-r','bakra-hisab-dist/Bakra-Hisab-1.3.3.apk')
launch_check();assert 'versionName=1.3.3' in adb('shell','dumpsys','package',pkg)
adb('shell','am','force-stop',pkg);launch_check()
print('PASS: signed 1.3.2 -> 1.3.3 in-place Android update preserves season, goat purchase, expense and employee records across restart.')
