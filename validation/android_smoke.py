import subprocess,time,xml.etree.ElementTree as ET,re,pathlib

def adb(*args):return subprocess.check_output(['adb',*args],text=True)
def tree():
 adb('shell','uiautomator','dump','/sdcard/window.xml')
 return ET.fromstring(adb('shell','cat','/sdcard/window.xml'))
def tap(text=None,classname=None):
 root=tree()
 for n in root.iter('node'):
  if (text and text in n.attrib.get('text','')) or (classname and n.attrib.get('class')==classname):
   b=list(map(int,re.findall(r'\d+',n.attrib['bounds'])));adb('shell','input','tap',str((b[0]+b[2])//2),str((b[1]+b[3])//2));time.sleep(1);return
 raise AssertionError('Control missing: '+str(text or classname)+' '+ET.tostring(root,encoding='unicode')[:5000])
def assert_text(text):
 xml=ET.tostring(tree(),encoding='unicode');assert text in xml,xml[:5000]
assert_text('Bakra Hisab');tap('Naya season');tap(classname='android.widget.EditText');adb('shell','input','text','Android-test');adb('shell','input','keyevent','4');time.sleep(1);tap('Preview');tap('Confirm aur save');assert_text('Android-test')
adb('shell','am','force-stop','com.bakrahisab.app');adb('shell','am','start','-W','-n','com.bakrahisab.app/.MainActivity');time.sleep(3);assert_text('Android-test')
adb('install','-r','Bakra-Hisab.apk');adb('shell','am','start','-W','-n','com.bakrahisab.app/.MainActivity');time.sleep(3);assert_text('Android-test')
print('PASS: Android install, native local save, force-stop/reopen, update in place preserves season.')
adb('shell','screencap','-p','/sdcard/screen.png');subprocess.check_call(['adb','pull','/sdcard/screen.png','android-screen.png'])
pathlib.Path('bakra-hisab-dist/android-window.xml').write_text(ET.tostring(tree(),encoding='unicode'))
