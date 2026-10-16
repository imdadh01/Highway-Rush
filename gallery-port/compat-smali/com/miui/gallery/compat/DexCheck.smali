.class public final Lcom/miui/gallery/compat/DexCheck;
.super Ljava/lang/Object;

.method public static main([Ljava/lang/String;)V
    .locals 7
    const-class v0, Lcom/miui/gallery/compat/DexCheck;
    invoke-virtual {v0}, Ljava/lang/Class;->getClassLoader()Ljava/lang/ClassLoader;
    move-result-object v0
    const-string v1, "com.miui.gallery.GalleryApp"
    const-string v2, "miui.util.FeatureParser"
    const-string v3, "com.xiaomi.micloudsdk.provider.MiCloudSettings"
    const-string v4, "com.miui.gallery.activity.HomePageActivity"
    filled-new-array {v1, v2, v3, v4}, [Ljava/lang/String;
    move-result-object v1
    const/4 v2, 0x0
    :next
    array-length v3, v1
    if-ge v2, v3, :done
    aget-object v3, v1, v2
    const/4 v4, 0x0
    invoke-static {v3, v4, v0}, Ljava/lang/Class;->forName(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;
    sget-object v4, Ljava/lang/System;->out:Ljava/io/PrintStream;
    new-instance v5, Ljava/lang/StringBuilder;
    invoke-direct {v5}, Ljava/lang/StringBuilder;-><init>()V
    const-string v6, "PASS: ART linked "
    invoke-virtual {v5, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v5, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v5}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v3
    invoke-virtual {v4, v3}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V
    add-int/lit8 v2, v2, 0x1
    goto :next
    :done
    sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;
    const-string v1, "PASS: Gallery DEX loading; full ARM64 app runtime remains a separate test"
    invoke-virtual {v0, v1}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V
    return-void
.end method
