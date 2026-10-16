import argparse, hashlib, json, re, subprocess, time
import xml.etree.ElementTree as ET
from pathlib import Path
from PIL import Image

PKG='com.miui.gallery'
OUT=Path('test-results');OUT.mkdir(exist_ok=True)

def adb(*args, check=True):
    p=subprocess.run(['adb',*args],capture_output=True,text=True,timeout=45)
    if check and p.returncode:raise RuntimeError(p.stdout+p.stderr)
    return p.stdout+p.stderr

def current():
    return '\n'.join(s for s in adb('shell','dumpsys','activity','activities').splitlines() if 'ResumedActivity' in s)

def nodes():
    adb('shell','uiautomator','dump','/sdcard/gallery-ui.xml')
    adb('pull','/sdcard/gallery-ui.xml',str(OUT/'ui.xml'))
    return list(ET.parse(OUT/'ui.xml').getroot().iter('node'))

def tap_node(n):
    b=list(map(int,re.findall(r'\d+',n.get('bounds',''))))
    if len(b)!=4:return False
    adb('shell','input','tap',str((b[0]+b[2])//2),str((b[1]+b[3])//2));return True

def click(text):
    for n in nodes():
        if n.get('text','').strip().casefold()==text.casefold():return tap_node(n)
    return False

def screenshot(name):
    p=subprocess.run(['adb','exec-out','screencap','-p'],capture_output=True,check=True,timeout=30)
    file=OUT/(name+'.png');file.write_bytes(p.stdout)
    return Image.open(file).convert('RGB')

def photo_point(img,color):
    points=[]
    for i,p in enumerate(img.getdata()):
        if all(abs(p[k]-color[k])<14 for k in range(3)):points.append((i%img.width,i//img.width))
    assert len(points)>max(2000,img.width*img.height//100),'Seeded local photo is not visibly rendered'
    return tuple(sum(p[k] for p in points)//len(points) for k in (0,1))

def run(apk):
    assert adb('shell','getprop','ro.build.version.sdk').strip()=='33'
    abi=adb('shell','getprop','ro.product.cpu.abilist').strip()
    OUT.joinpath('abi.txt').write_text(abi)
    assert 'arm64-v8a' in abi,'This emulator cannot execute ARM64 APK libraries'
    OUT.joinpath('install.txt').write_text(adb('install','-r','-g',str(apk)))
    adb('shell','cmd','appops','set',PKG,'MANAGE_EXTERNAL_STORAGE','allow')
    fixtures=OUT/'fixtures';fixtures.mkdir(exist_ok=True)
    colors=[(220,40,50),(40,190,100)]
    adb('shell','mkdir','-p','/sdcard/DCIM/NubiaTest')
    for i,color in enumerate(colors):
        p=fixtures/f'Nubia-test-{i}.png';Image.new('RGB',(320,240),color).save(p)
        remote='/sdcard/DCIM/NubiaTest/'+p.name;adb('push',str(p),remote)
        adb('shell','am','broadcast','-a','android.intent.action.MEDIA_SCANNER_SCAN_FILE','-d','file://'+remote)
    adb('logcat','-c')
    launch=adb('shell','am','start','-W','-n',PKG+'/com.miui.gallery.activity.HomePageActivity')
    OUT.joinpath('launch.txt').write_text(launch)
    for i in range(25):
        ns=nodes();handled=False
        for n in ns:
            text=n.get('text','').strip().casefold()
            if n.get('enabled')=='true' and text in ['agree','continue','allow','got it','ok','agree and continue']:
                tap_node(n);handled=True;break
        if not handled and 'HomePageActivity' in current():break
        time.sleep(2)
    assert 'HomePageActivity' in current(),'Gallery home did not start'
    point=None;rendered=False
    for i in range(30):
        try:
            img=screenshot('photos');point=photo_point(img,colors[0]);photo_point(img,colors[1]);rendered=True;break
        except AssertionError:time.sleep(2)
    assert rendered,'Local test photos did not appear'
    adb('shell','input','tap',str(point[0]),str(point[1]));time.sleep(3)
    assert 'PhotoPageActivity' in current(),'Local photo viewer did not open'
    screenshot('photo-open');adb('shell','input','keyevent','4');time.sleep(2)
    assert click('Albums'),'Albums tab is missing'
    time.sleep(3);screenshot('albums')
    assert any('NubiaTest' in n.get('text','') for n in nodes()),'Seeded local album is missing'
    time.sleep(30)
    assert PKG in current(),'Gallery stopped after interaction'
    logs=adb('logcat','-d','-b','crash');OUT.joinpath('crash.log').write_text(logs)
    assert 'Process: '+PKG not in logs,'Gallery process crashed'
    OUT.joinpath('result.json').write_text(json.dumps({'status':'passed','android':13,'abi':abi,'sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),'checks':['install','home','local-photo-rendering','photo-viewer','albums','stability']},indent=2))

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('apk',type=Path);args=parser.parse_args()
    try:run(args.apk)
    except Exception as error:
        OUT.joinpath('result.json').write_text(json.dumps({'status':'failed','error':str(error)},indent=2))
        OUT.joinpath('crash.log').write_text(adb('logcat','-d','-b','crash',check=False))
        try:screenshot('failure')
        except Exception:pass
        raise
