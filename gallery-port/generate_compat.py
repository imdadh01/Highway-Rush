from pathlib import Path
import re, struct, json

root = Path('work/gallery')
shimroot = root / 'smali_classes5'
changes = []

def method(sig, body, static=True, locals=1):
    return f'.method public {"static " if static else ""}{"constructor " if sig.startswith(("<init>", "<clinit>")) else ""}{sig}\n    .locals {locals}\n{body}\n.end method\n'

def cls(name, methods='', fields='', parent='java/lang/Object', interfaces=''):
    p = shimroot / (name + '.smali')
    p.parent.mkdir(parents=True, exist_ok=True)
    ctor = method('<init>()V', f'    invoke-direct {{p0}}, L{parent};-><init>()V\n    return-void', False, 0)
    p.write_text(f'.class public L{name};\n.super L{parent};\n{interfaces}\n{fields}\n{ctor}\n{methods}')
    changes.append('Compatibility adapter: ' + name)

zero = '    const/4 v0, 0x0\n    return v0'
null = '    const/4 v0, 0x0\n    return-object v0'
void = '    return-void'
widezero = '    const-wide/16 v0, 0x0\n    return-wide v0'

# Hardware detection: use the real Android device and report no Xiaomi-only features.
flags = ['IS_TABLET','IS_MIPAD','IS_ALPHA_BUILD','IS_DEVELOPMENT_VERSION','IS_STABLE_VERSION','IS_INTERNATIONAL_BUILD','IS_GLOBAL_BUILD']
fields = '\n'.join(f'.field public static final {f}:Z = {"true" if f in ("IS_STABLE_VERSION","IS_INTERNATIONAL_BUILD","IS_GLOBAL_BUILD") else "false"}' for f in flags)
fields += '\n.field public static DEVICE:Ljava/lang/String;\n'
methods = '.method static constructor <clinit>()V\n    .locals 1\n    sget-object v0, Landroid/os/Build;->DEVICE:Ljava/lang/String;\n    sput-object v0, Lmiui/os/Build;->DEVICE:Ljava/lang/String;\n    return-void\n.end method\n'
methods += method('getRegion()Ljava/lang/String;', '    invoke-static {}, Ljava/util/Locale;->getDefault()Ljava/util/Locale;\n    move-result-object v0\n    invoke-virtual {v0}, Ljava/util/Locale;->getCountry()Ljava/lang/String;\n    move-result-object v0\n    return-object v0')
cls('miui/os/Build', methods, fields)
cls('miui/util/FeatureParser',
    method('getBoolean(Ljava/lang/String;Z)Z', '    return p1', locals=0) +
    method('getInteger(Ljava/lang/String;I)I', '    return p1', locals=0) +
    method('getString(Ljava/lang/String;)Ljava/lang/String;', '    const-string v0, ""\n    return-object v0') +
    method('getIntArray(Ljava/lang/String;)[I', null) +
    method('hasFeature(Ljava/lang/String;I)Z', zero))
cls('miui/accounts/ExtraAccountManager', method('getXiaomiAccount(Landroid/content/Context;)Landroid/accounts/Account;', null))
cls('miui/cloud/os/SystemProperties',
    method('get(Ljava/lang/String;)Ljava/lang/String;', '    const-string v0, ""\n    invoke-static {p0, v0}, Lmiui/external/SdkHelper;->getSystemProperty(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;\n    move-result-object v0\n    return-object v0') +
    method('get(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;', '    invoke-static {p0, p1}, Lmiui/external/SdkHelper;->getSystemProperty(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;\n    move-result-object v0\n    return-object v0'))
cls('miui/cloud/os/ServiceManager', method('getService(Ljava/lang/String;)Landroid/os/IBinder;', null))
cls('miui/hardware/display/DisplayFeatureManager',
    method('getInstance()Lmiui/hardware/display/DisplayFeatureManager;', '    new-instance v0, Lmiui/hardware/display/DisplayFeatureManager;\n    invoke-direct {v0}, Lmiui/hardware/display/DisplayFeatureManager;-><init>()V\n    return-object v0') + method('setScreenEffect(II)V', void, False, 0))
cls('miui/util/HapticFeedbackUtil',
    method('<init>(Landroid/content/Context;Z)V', '    invoke-direct {p0}, Ljava/lang/Object;-><init>()V\n    return-void', False, 0) +
    ''.join(method(s, zero, static) for s,static in [
        ('isSupportLinearMotorVibrate()Z', True), ('isSupportLinearMotorVibrate(I)Z', True),
        ('performExtHapticFeedback(I)Z', False), ('performHapticFeedback(IZ)Z',False),
        ('performHapticFeedback(Landroid/os/VibrationAttributes;IZ)Z',False)]))
cls('miui/provider/ExtraNetwork', method('isMobileRestrict(Landroid/content/Context;Ljava/lang/String;)Z', zero) + method('isWifiRestrict(Landroid/content/Context;Ljava/lang/String;)Z', zero))
cls('miui/app/MiuiFreeFormManager', method('getAllFreeFormStackInfosOnDisplay(I)Ljava/util/List;', '    invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;\n    move-result-object v0\n    return-object v0'))
cls('miui/app/MiuiFreeFormManager$MiuiFreeFormStackInfo', fields='.field public packageName:Ljava/lang/String;')
cls('miui/securityspace/CrossUserUtils', method('checkUidPermission(Landroid/content/Context;Ljava/lang/String;)Z', '    invoke-virtual {p0, p1}, Landroid/content/Context;->checkSelfPermission(Ljava/lang/String;)I\n    move-result v0\n    if-nez v0, :denied\n    const/4 v0, 0x1\n    return v0\n    :denied\n    const/4 v0, 0x0\n    return v0'))
cls('miui/content/pm/PreloadedAppPolicy', method('installPreloadedDataApp(Landroid/content/Context;Ljava/lang/String;Landroid/content/pm/IPackageInstallObserver2;I)Z', zero))
cls('miui/util/ReflectionUtils', method('getStaticObjectField(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Object;', '    :try_start\n    invoke-virtual {p0, p1}, Ljava/lang/Class;->getDeclaredField(Ljava/lang/String;)Ljava/lang/reflect/Field;\n    move-result-object v0\n    const/4 v1, 0x1\n    invoke-virtual {v0, v1}, Ljava/lang/reflect/Field;->setAccessible(Z)V\n    const/4 v1, 0x0\n    invoke-virtual {v0, v1}, Ljava/lang/reflect/Field;->get(Ljava/lang/Object;)Ljava/lang/Object;\n    move-result-object v0\n    :try_end\n    .catch Ljava/lang/Exception; {:try_start .. :try_end} :failed\n    return-object v0\n    :failed\n    move-exception v0\n    const/4 v0, 0x0\n    return-object v0', locals=2))

# Missing cloud classes remain linkable, but no cloud account or sync is advertised.
cls('com/xiaomi/micloudsdk/utils/AESStringDef$InvalidAESDataException', method('<init>(Ljava/lang/String;)V', '    invoke-direct {p0, p1}, Ljava/lang/Exception;-><init>(Ljava/lang/String;)V\n    return-void', False, 0), parent='java/lang/Exception')
cls('com/xiaomi/micloudsdk/utils/IXiaomiAccountServiceProxy', method('getEncryptedUserId(Landroid/os/IBinder;Landroid/accounts/Account;)Ljava/lang/String;', null))
cls('com/xiaomi/micloudsdk/provider/MiCloudSettings',
    method('getInt(Landroid/content/ContentResolver;Ljava/lang/String;I)I','    return p2',locals=0) +
    method('getString(Landroid/content/ContentResolver;Ljava/lang/String;)Ljava/lang/String;',null) +
    method('putInt(Landroid/content/ContentResolver;Ljava/lang/String;I)Z',zero) +
    method('putString(Landroid/content/ContentResolver;Ljava/lang/String;Ljava/lang/String;)Z',zero))
cls('com/xiaomi/micloudsdk/sync/utils/SyncContentUtils',
    method('recordSyncResult(Landroid/content/Context;Ljava/lang/String;I)V',void,locals=0) +
    method('isPauseExceptAuthority(Landroid/content/Context;Ljava/lang/String;)Z','    const/4 v0, 0x1\n    return v0') +
    method('loadResumeTime(Landroid/content/Context;Ljava/lang/String;)J',widezero,locals=2) +
    method('savePauseTime(Landroid/content/Context;Ljava/lang/String;J)V',void,locals=0) +
    method('insertPauseExceptAuthority(Landroid/content/Context;Ljava/lang/String;)V',void,locals=0))
cls('miui/cloud/content/MiSyncPolicyResolver',
    method('setMiSyncPauseToTime(Landroid/accounts/Account;J)V',void,locals=0) +
    method('getMiSyncPauseToTime(Landroid/accounts/Account;)J',widezero,locals=2) +
    method('setMiSyncResume(Landroid/accounts/Account;)V',void,locals=0))
sync_methods=['getSyncedCount','getUnSyncedSecretCount','getUnsyncedCount','getWifiOnlyUnsyncedCount']
p=shimroot/'miui/cloud/sync/SyncInfoProvider.smali'; p.parent.mkdir(parents=True,exist_ok=True)
p.write_text('.class public interface abstract Lmiui/cloud/sync/SyncInfoProvider;\n.super Ljava/lang/Object;\n'+''.join(f'.method public abstract {m}(Landroid/content/Context;)I\n.end method\n' for m in sync_methods))
cls('miui/cloud/sync/SyncInfoProviderBase', ''.join(method(m+'(Landroid/content/Context;)I',zero,False) for m in sync_methods)+method('isDebug()Z',zero,False)+method('queryCount(Landroid/content/Context;Landroid/net/Uri;Ljava/lang/String;[Ljava/lang/String;)I',zero,False),interfaces='.implements Lmiui/cloud/sync/SyncInfoProvider;')
for name in ['ContactsSyncInfoProvider','SmsSyncInfoProvider','GallerySyncInfoProvider','CalllogSyncInfoProvider','NotesSyncInfoProvider','WifiSyncInfoProvider','SoundRecorderSyncInfoProvider','BrowserSyncInfoProvider','AntispamSyncInfoProvider','CalendarSyncInfoProvider','PersonalAssistantSyncInfoProvider','QuickSearchBoxProvider']:
    cls('miui/cloud/sync/providers/'+name,parent='miui/cloud/sync/SyncInfoProviderBase')

def replace_method(path, signature, body, locals=1):
    p=next(root.glob('smali*/'+path+'.smali'))
    s=p.read_text()
    pat=r'(?m)^\.method ([^\n]* '+re.escape(signature)+r')\n.*?^\.end method'
    s,n=re.subn(pat,lambda m:'.method '+m[1]+f'\n    .locals {locals}\n'+body+'\n.end method',s,flags=re.S)
    assert n==1,(path,signature,n)
    p.write_text(s); changes.append('Local mode: '+path+'->'+signature)

replace_method('com/miui/gallery/cloud/AccountCache','getAccount()Landroid/accounts/Account;',null)
replace_method('com/miui/gallery/util/SyncUtil','isGalleryCloudSyncable(Landroid/content/Context;)Z',zero)
replace_method('com/miui/gallery/push/GalleryPushManager','registerPush(Landroid/content/Context;)V',void,0)

# Remove only the required Xiaomi cloud shared-library node, preserve all resources.
b=bytearray((root/'AndroidManifest.xml').read_bytes()); strings=[]; chunks=[]; pos=8
u16=lambda q:struct.unpack_from('<H',b,q)[0]
u32=lambda q:struct.unpack_from('<I',b,q)[0]
while pos<len(b):
    t,h,n=struct.unpack_from('<HHI',b,pos)
    if t==1:
        count=u32(pos+8); utf8=u32(pos+16)&256; start=pos+u32(pos+20)
        for i in range(count):
            q=start+u32(pos+h+i*4)
            if utf8:
                def length(q):
                    v=b[q];return ((((v&127)<<8)|b[q+1],q+2) if v&128 else (v,q+1))
                _,q=length(q); l,q=length(q); s=b[q:q+l].decode('utf8')
            else:
                l=u16(q);q+=2
                if l&32768:l=((l&32767)<<16)|u16(q);q+=2
                s=b[q:q+l*2].decode('utf-16le')
            strings.append(s)
    chunks.append((pos,t,h,n));pos+=n
out=[];skip=0; manifest_changes=[]
for pos,t,h,n in chunks:
    c=bytearray(b[pos:pos+n])
    if t==258:
        tag=strings[u32(pos+20)];aoff=u16(pos+24);asz=u16(pos+26);count=u16(pos+28);attrs={}; offsets={}
        for i in range(count):
            q=pos+16+aoff+i*asz;name=strings[u32(q+4)];raw=u32(q+8);typ=b[q+15];v=u32(q+16)
            attrs[name]=strings[raw] if raw!=0xffffffff else strings[v] if typ==3 else v
            offsets[name]=q-pos
        if tag=='uses-library' and attrs.get('name')=='micloud-sdk':
            skip=1;manifest_changes.append('Removed mandatory micloud-sdk');continue
        if tag=='application':
            for attr in ('allowBackup','fullBackupOnly'):
                if attr in offsets:
                    q=offsets[attr];struct.pack_into('<I',c,q+8,0xffffffff);c[q+15]=18;struct.pack_into('<I',c,q+16,0)
                    manifest_changes.append(attr+'=false')
            # Remove backupAgent so Android never attempts the Xiaomi backup adapter.
            if 'backupAgent' in offsets:
                q=offsets['backupAgent']; del c[q:q+asz];struct.pack_into('<I',c,4,len(c));struct.pack_into('<H',c,28,count-1)
                # Attribute indices are one based; shift any special indices after deletion.
                removed=(q-16-aoff)//asz+1
                for index_off in (30,32,34):
                    index=struct.unpack_from('<H',c,index_off)[0]
                    if index>=removed:struct.pack_into('<H',c,index_off,0 if index==removed else index-1)
                manifest_changes.append('Removed Xiaomi backupAgent')
    elif t==259 and skip:
        skip=0;continue
    if not skip:out.append(bytes(c))
result=bytearray(b[:8]+b''.join(out));struct.pack_into('<I',result,4,len(result));(root/'AndroidManifest.xml').write_bytes(result)
changes+=manifest_changes
Path('work/port_changes.json').write_text(json.dumps(changes,indent=2))
print('\n'.join(changes))
