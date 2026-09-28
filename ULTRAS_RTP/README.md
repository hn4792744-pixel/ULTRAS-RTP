# ULTRAS_RTP

**Developer:** UC_Hussein • Premium safe random teleport for Paper.

> ⚠️ المشروع لم يُبنَ (compile) داخل بيئة الإنشاء لعدم توفر الإنترنت لتحميل Paper API. نفّذ `gradle build` عندك وأخبرني بأي خطأ ليُصلَح فورًا.

## المتطلبات
| الهدف | Java | Paper API (`gradle.properties`) |
|---|---|---|
| **الافتراضي** | 25 | `26.1.2.build.+` (Paper الحديث يتطلب Java 25) |
| Minecraft 1.21.x | 21 | `targetJava=21` و `paperVersion=1.21.11-R0.1-SNAPSHOT` |

الكود يستخدم Paper API الرسمية فقط (بدون NMS) ونفس الكود يصلح للحالتين. جار plugin.yml بـ `api-version: 1.21`؛ إن رفضه سيرفرك غيّر `apiVersion` في `gradle.properties`.

## التثبيت
1. `gradle build` (Gradle 9.1+ لدعم Java 25) ← الناتج `build/libs/ULTRAS_RTP-1.0.0.jar`.
2. ضعه في `plugins/` ثم شغّل السيرفر. ستُنشأ: `config.yml`, `sounds.yml`, `languages/`, `guis/rtp.yml`.

## الأوامر
| الأمر | الوصف |
|---|---|
| `/rtp` | انتقال عشوائي (أو GUI إذا `gui.enabled: true`) |
| `/rtp reload` | إعادة تحميل كل شيء بدون Restart (صلاحية فقط، لا يظهر في TAB لغير المصرّح) |
| `/rtp sounds` | اختياري: تشغيل/إيقاف أصواتك (يُفعَّل من `player-sound-toggle.enabled`، محفوظ بـ UUID) |

## الصلاحيات
`ultras.rtp.use` (الكل) • `ultras.rtp.reload` (OP) • `ultras.rtp.bypass.cooldown` (OP) • `ultras.rtp.bypass.move` (OP) • `ultras.rtp.admin` (OP، يشمل الكل).
أسماء الصلاحيات قابلة للتغيير من `permissions:` في config.yml (الأسماء المخصصة تحتاج إعطاءها عبر Permission plugin). ولكل عالم صلاحية اختيارية `permission:`.

## الإعدادات والعوالم
كل عالم تحت `worlds:` بإعداداته المستقلة (`enabled, min-radius, max-radius, center-x/z, cooldown, countdown, cancel-on-move, movement-threshold, max-attempts, allow-water, height-mode, blocked-biomes, permission`). أي قيمة ناقصة تؤخذ من `defaults:`. لإضافة عالم جديد أضف قسمًا باسم مجلد العالم.
- `cooldown.start-on: SUCCESS|START` — متى يبدأ الـCooldown.
- `height-mode`: `HIGHEST` (أعلى بلوك، يتجاهل الأوراق) • `SCAN` (مسح من `scan-max-y` إلى `scan-min-y`) • `AUTO` (Nether=SCAN، الباقي HIGHEST).
- الاختيار العشوائي منتظم على الحلقة `min-radius..max-radius` (بدون انحياز للمركز).

## نظام Fallback
1. عالم اللاعب مفعّل ← RTP داخله.
2. غير مفعّل ← إن كان للعالم `fallback-to-world` يُستخدم، وإلا `fallback.enabled`.
3. مفعّل ← `fallback.target-world` (يجب أن يكون مفعّلًا ومحمّلًا).
4. غير مفعّل ← رسالة «غير متاح هنا». عالم هدف غير صالح ← رسالة خطأ آمنة.

مثال: اللاعب في `lobby` ← `/rtp` ← ينتقل عشوائيًا في `world`. Nether/End معطّلان افتراضيًا مع `fallback-to-world: true`.

## GUI
`gui.enabled: true` يفتح GUI يعرض فقط العوالم (المفعّلة + المحمّلة + غير المخفية). إخفاء عالم من GUI فقط: `gui.worlds.<world>.enabled: false`. التصميم كاملًا في `guis/rtp.yml` (Title, Size, Materials, Names, Lore, Custom Model Data, Enchants, Glow, Filler, Border). الأصوات في `sounds.yml`. يُعاد التحقق من العالم عند الضغط، ويُغلق GUI عند Reload.

## الأصوات واللغات
`sounds.yml`: لكل حدث `sound / volume / pitch` (`sound: none` للصمت). اللغات: `languages/en.yml` و`ar.yml` (MiniMessage) — غيّر `language:` ثم `/rtp reload`. يمكنك إضافة لغة بنسخ `en.yml`.

## Safe RTP
يُرفض أي موقع: Lava/Fire/Cactus/Magma/Campfire/Powder Snow/Berry Bush/Cobweb/بوابات، ماء (إلا `allow-water`)، فراغ (Void)، داخل بلوكات، فوق أوراق الشجر، خارج World Border، أو بجانب خطر. يُفحص الأرض + مكان القدمين + الرأس. المحاولات = `max-attempts`. الـChunks تُحمَّل بـ `getChunkAtAsync` ولا يُجمَّد الـMain Thread. *لا يوجد فحص للـStructures*؛ استخدم `blocked-biomes` أو World Border لتجنب مناطق معينة.

## تعارض الأمر /rtp
- `plugin.yml` يحمّل ULTRAS_RTP قبل BetterRTP/RandomTeleport/RTP ليسجّل `/rtp` أولًا.
- إن سبقه Plugin آخر: مع `command.override-conflicts: true` يعالج ULTRAS_RTP رسالة `/rtp` للاعبين عبر `PlayerCommandPreprocessEvent` (حدث Bukkit عام) دون تعطيل أو تعديل الـPlugin الآخر، ولا يمسّ أي أمر آخر.
- **حدود معروفة:** TAB Completion يبقى للأمر المسجَّل أولًا، ويمكن دائمًا استخدام `/ultras_rtp:rtp`. الحل الأنظف: إزالة/تعطيل `/rtp` في الـPlugin الآخر من إعداداته.

## الحالات الحدّية
خروج اللاعب / موته / تغيير عالمه أثناء العدّ ← إلغاء. تنفيذ `/rtp` مرتين ← رفض (جلسة واحدة لكل لاعب). Reload أثناء العدّ ← تستمر الجلسة وتُقرأ إعدادات العالم الجديدة عند الانتقال. Shutdown ← تُلغى المهام وتُحفظ البيانات.

## Troubleshooting
- `Missing message: ...` ← مفتاح ناقص في ملف اللغة (يُستخدم en.yml كبديل).
- «No safe location» ← زد `max-attempts` أو صغّر النطاق أو تحقق من World Border.
- Sounds لا تعمل ← استخدم مفاتيح بنقاط مثل `block.amethyst_block.chime`.
- الأمر لا يظهر ← تأكد من `ultras.rtp.use` وأن Plugin آخر لا يأخذ `/rtp`.

## Build
```
gradle build            # Java 25 toolchain
```
