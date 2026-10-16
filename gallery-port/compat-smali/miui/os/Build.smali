.class public Lmiui/os/Build;
.super Ljava/lang/Object;

.field public static final IS_TABLET:Z = false
.field public static final IS_MIPAD:Z = false
.field public static final IS_ALPHA_BUILD:Z = false
.field public static final IS_DEVELOPMENT_VERSION:Z = false
.field public static final IS_STABLE_VERSION:Z = true
.field public static final IS_INTERNATIONAL_BUILD:Z = true
.field public static final IS_GLOBAL_BUILD:Z = true
.field public static DEVICE:Ljava/lang/String;

.method public constructor <init>()V
    .locals 0
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method static constructor <clinit>()V
    .locals 1
    sget-object v0, Landroid/os/Build;->DEVICE:Ljava/lang/String;
    sput-object v0, Lmiui/os/Build;->DEVICE:Ljava/lang/String;
    return-void
.end method
.method public static getRegion()Ljava/lang/String;
    .locals 1
    invoke-static {}, Ljava/util/Locale;->getDefault()Ljava/util/Locale;
    move-result-object v0
    invoke-virtual {v0}, Ljava/util/Locale;->getCountry()Ljava/lang/String;
    move-result-object v0
    return-object v0
.end method
