import subprocess,time,re,pathlib,csv,io
from PIL import Image,ImageOps

def adb(*args):return subprocess.check_output(['adb',*args],text=True)
def screen():
 subprocess.run(['adb','shell','screencap','-p','/sdcard/test.png'],check=True)
 subprocess.run(['adb','pull','/sdcard/test.png','test-screen.png'],check=True,stdout=subprocess.DEVNULL)
 ImageOps.invert(Image.open('test-screen.png').convert('RGB')).save('test-inverted.png')
 normal=subprocess.check_output(['tesseract','test-screen.png','stdout','--psm','11','tsv'],text=True,stderr=subprocess.DEVNULL)
 inverted=subprocess.check_output(['tesseract','test-inverted.png','stdout','--psm','11','tsv'],text=True,stderr=subprocess.DEVNULL)
 return normal+'\n'+'\n'.join(inverted.splitlines()[1:])
def words():return [x for x in csv.DictReader(io.StringIO(screen()),delimiter='\t') if x.get('text','').strip()]
def norm(s):return re.sub('[^a-z0-9]','',s.lower())
def tap(label,dy=0):
 wanted=norm(label);rows=words()
 for i in range(len(rows)):
  for length in range(1,6):
   group=rows[i:i+length]
   if norm(' '.join(x['text'] for x in group))==wanted:
    left=min(int(x['left']) for x in group);right=max(int(x['left'])+int(x['width']) for x in group);top=min(int(x['top']) for x in group);bottom=max(int(x['top'])+int(x['height']) for x in group)
    adb('shell','input','tap',str((left+right)//2),str((top+bottom)//2+dy));time.sleep(1);return
 raise AssertionError('Control missing: '+label+'; OCR: '+' '.join(x['text'] for x in rows))
def tap_green():
 screen()
 im=Image.open('test-screen.png').convert('RGB');w,h=im.size;seen=set();regions=[]
 for y in range(h):
  for x in range(w):
   if (x,y) in seen:continue
   r,g,b=im.getpixel((x,y))
   if not(r<40 and g>60 and g>r*1.8 and 25<b<145):continue
   todo=[(x,y)];seen.add((x,y));pts=[]
   while todo:
    a,c=todo.pop();pts.append((a,c))
    for p,q in ((a+1,c),(a-1,c),(a,c+1),(a,c-1)):
     if p<0 or q<0 or p>=w or q>=h or (p,q) in seen:continue
     r,g,b=im.getpixel((p,q))
     if r<40 and g>60 and g>r*1.8 and 25<b<145:seen.add((p,q));todo.append((p,q))
   if len(pts)>700:
    left=min(p[0] for p in pts);right=max(p[0] for p in pts);top=min(p[1] for p in pts);bottom=max(p[1] for p in pts)
    if right-left>70 and bottom-top>25:regions.append((left,top,right,bottom))
 if not regions:raise AssertionError('Green action button not found')
 left,top,right,bottom=max(regions,key=lambda r:r[3])
 adb('shell','input','tap',str((left+right)//2),str((top+bottom)//2));time.sleep(1)
def assert_text(text):
 for _ in range(3):
  actual=' '.join(x['text'] for x in words())
  if norm(text) in norm(actual):return
  time.sleep(2)
 raise AssertionError('Text missing: '+text+'; OCR: '+actual)
adb('shell','settings','put','secure','show_ime_with_hard_keyboard','1')
assert_text('Bakra Hisab');tap_green();tap('Season ka naam',35);adb('shell','input','text','Android-test');time.sleep(1);adb('shell','input','keyevent','4');time.sleep(1);tap_green();tap_green();assert_text('Android-test')
adb('shell','am','force-stop','com.bakrahisab.app');adb('shell','am','start','-W','-n','com.bakrahisab.app/.MainActivity');time.sleep(4);assert_text('Android-test')
adb('install','-r','Bakra-Hisab.apk');adb('shell','am','start','-W','-n','com.bakrahisab.app/.MainActivity');time.sleep(4);assert_text('Android-test')
print('PASS: Android install, native local save, force-stop/reopen, update in place preserves season.',flush=True)
adb('shell','screencap','-p','/sdcard/screen.png');subprocess.check_call(['adb','pull','/sdcard/screen.png','android-screen.png'])
pathlib.Path('bakra-hisab-dist/android-verification.txt').write_text('PASS: Android 35 install, native season save, force-stop/reopen and install -r preserve data.\n')
