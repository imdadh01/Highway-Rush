.class public Lmiui/securityspace/CrossUserUtils;
.super Ljava/lang/Object;


.method public constructor <init>()V
    .locals 0
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public static checkUidPermission(Landroid/content/Context;Ljava/lang/String;)Z
    .locals 1
    invoke-virtual {p0, p1}, Landroid/content/Context;->checkSelfPermission(Ljava/lang/String;)I
    move-result v0
    if-nez v0, :denied
    const/4 v0, 0x1
    return v0
    :denied
    const/4 v0, 0x0
    return v0
.end method
