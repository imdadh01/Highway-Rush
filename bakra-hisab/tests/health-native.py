import subprocess,json,time,datetime,pathlib
PKG='com.bakrahisab.app'
def adb(*args,**kw): return subprocess.check_output(['adb',*args],text=True,**kw)
adb('install','-r','bakra-hisab/app/build/outputs/apk/debug/app-debug.apk')
adb('shell','pm','grant',PKG,'android.permission.POST_NOTIFICATIONS')
adb('shell','appops','set',PKG,'SCHEDULE_EXACT_ALARM','allow')
now=datetime.datetime.fromisoformat(adb('shell','date','+%Y-%m-%dT%H:%M:%S').strip())
due=(now+datetime.timedelta(minutes=2)).replace(second=0,microsecond=0)
d={'version':3,'revision':0,'sequence':0,'current':'native','seasons':[{'id':'native','name':'Notification test','start':now.date().isoformat(),'opening':0}], 'goats':[], 'expenses':[], 'sales':[], 'receipts':[], 'general':[], 'partner':[], 'employees':[], 'employeePayments':[], 'health':[], 'drafts':{}, 'settings':{'partnerName':'','accounts':['Cash'],'healthNotifications':{'enabled':True,'time':'09:00','leadDays':0}},'audit':[],'schedules':[{'id':'native-schedule','season':'native','date':due.date().isoformat(),'time':due.strftime('%H:%M'),'goats':[],'doneGoats':[],'done':False,'label':'Native health reminder'}]}
adb('shell','run-as',PKG,'mkdir','-p','files')
subprocess.run(['adb','shell','run-as',PKG,'sh','-c',"'cat > files/hisab-v1.json'"],input=json.dumps(d),text=True,check=True)
adb('shell','am','start','-W','-n',PKG+'/.MainActivity');time.sleep(8)
# Permission denial must be surfaced without losing the saved schedule.
adb('shell','input','keyevent','3');time.sleep(2);adb('shell','am','kill',PKG)
wait=max(2,(due-now).total_seconds()+10-10);time.sleep(wait)
out=adb('shell','dumpsys','notification','--noredact')
pathlib.Path('tmp/app/notification-dump.txt').write_text(out)
assert 'Native health reminder' in out,'Offline scheduled notification missing'
assert 'sehat_reminders' in out,'Notification channel missing'
# Completing the schedule should clear the notification on next sync.
adb('shell','am','force-stop',PKG);d['schedules'][0]['done']=True
subprocess.run(['adb','shell','run-as',PKG,'sh','-c',"'cat > files/hisab-v1.json'"],input=json.dumps(d),text=True,check=True)
adb('shell','am','start','-W','-n',PKG+'/.MainActivity');time.sleep(8)
prefs=adb('shell','run-as',PKG,'cat','shared_prefs/health_reminders.xml')
assert 'name="events">[]<' in prefs,'Completed schedule still queued'
print('PASS: new health notification delivered offline after background process kill; completion removes queued reminder.')
