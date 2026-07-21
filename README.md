# Rust+ Custom Android App

## Ko TRŪKSTA prieš kompiliuojant (svarbu!)

Šiame ZIP archyve yra visas kodas, bet dėl techninių apribojimų (nebuvo interneto
prieigos ir binarinių failų generavimo šioje sesijoje) TRŪKSTA 3 dalykų:

1. **`gradle/wrapper/gradle-wrapper.jar`** — binarinis failas, kurio negalėjau
   sugeneruoti. Sprendimas: atidarykite projektą **Android Studio** — jis
   automatiškai sugeneruos šį failą per "Gradle Sync" (arba paleiskite
   `gradle wrapper` komandą, jei turite Gradle įdiegtą kompiuteryje).
   Vėliau šis failas atsiras ir GitHub Actions build'as veiks pilnai.

2. **`app/google-services.json`** — tikras Firebase konfigūracijos failas.
   Šiuo metu yra tik `app/google-services.json.PLACEHOLDER` su instrukcijomis.
   Žr. skyrių žemiau "Firebase nustatymas".

3. **`app/src/main/proto/rustplus.proto`** — šiuo metu yra tik placeholder su
   instrukcijomis. Tikrą failą reikia atsisiųsti iš
   https://github.com/liamcottle/rustplus.js ir įdėti čia, TIKSLIAI tuo pačiu
   pavadinimu (`rustplus.proto`), pakeičiant placeholder turinį.

4. **`app/src/main/res/raw/alarm.wav`** — įdėtas TIK paprastas pyptelėjimo
   garsas kaip vietos žymeklis. Galite palikti arba pakeisti savo pasirinktu
   .mp3/.wav failu (pervadinkite į `alarm.mp3` arba `alarm.wav` ir atitinkamai
   patikrinkite `AlarmSoundService.kt`, jis naudoja `R.raw.alarm`, tad
   pavadinimas be plėtinio turi būti `alarm`).

Be šių 3 dalykų (proto, google-services.json, gradle-wrapper.jar) projektas
NESIKOMPILIUOS. Tai yra normalu — šie failai yra unikalūs kiekvienam
Firebase projektui / negali būti sugeneruoti be interneto prieigos.

## Kaip kompiliuoti į APK (rekomenduojamas būdas per telefoną)

### 1 žingsnis — GitHub repo
1. Atsisiųskite šį ZIP ir išskleiskite.
2. Sutvarkykite 3 trūkstamus dalykus aukščiau (bent jau proto ir
   google-services.json — be jų build tikrai nepavyks).
3. Sukurkite naują repo GitHub'e (per telefono naršyklę arba GitHub app).
4. Įkelkite VISUS failus į repo (įskaitant paslėptą `.github/` katalogą).

### 2 žingsnis — Automatinis build
Repo jau turi paruoštą `.github/workflows/build.yml`. Vos tik "pushinsite"
kodą į `main` šaką, GitHub automatiškai:
- atsisiųs Java/Gradle,
- sukompiliuos projektą,
- paruoš `app-debug.apk` failą atsisiuntimui.

### 3 žingsnis — Atsisiųsti APK
1. Repo → **Actions** skiltis (telefone per GitHub app arba m.github.com).
2. Paspauskite paskutinį (žalią varnelę turintį) build'ą.
3. Apačioje "Artifacts" → atsisiųskite `app-debug.apk`.
4. Atidarykite failą telefone → leiskite "Install from unknown sources" →
   įdiekite.

## Firebase nustatymas (būtina FCM/Smart Alarm veikimui)

1. https://console.firebase.google.com/ → "Add project"
2. Projekto viduje: "Add app" → Android
3. Package name įveskite: `com.example.rustplus`
   (tiksliai toks pat, koks yra `app/build.gradle.kts` faile `applicationId`)
4. Atsisiųskite pasiūlytą `google-services.json`
5. Įdėkite šį failą į `app/google-services.json` (pakeisdami placeholder)

## Svarbios pastabos apie patį API

- Rust+ API yra **neoficialus** — Facepunch jo nedokumentuoja viešai.
- Norint gauti `playerToken`, reikalinga FCM registracija ir "pairing" per
  žaidimą (ESC → Rust+ → Pair with Server). Rekomenduojama pirmiausia
  pasižiūrėti, kaip tai daro `liamcottle/rustplus.js` arba `rustplusplus`
  projektai, nes ten yra veikianti pairing/FCM registracijos logika, kurios
  šis Android projektas kol kas neturi pilnai automatizuotos.
- API gali pasikeisti bet kada be įspėjimo — nėra garantijų dėl stabilumo.

## Projekto struktūra

```
RustPlusCustom/
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew / gradlew.bat
├── .github/workflows/build.yml
├── app/
│   ├── build.gradle.kts
│   ├── google-services.json.PLACEHOLDER   ← pakeisti tikru failu
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/rustplus/
│       │   ├── MainActivity.kt
│       │   ├── SteamLoginActivity.kt
│       │   ├── RustPlusClient.kt
│       │   ├── FcmService.kt
│       │   └── AlarmSoundService.kt
│       ├── proto/rustplus.proto           ← pakeisti tikru failu
│       └── res/
│           ├── layout/activity_main.xml
│           ├── values/(strings.xml, themes.xml)
│           ├── raw/alarm.wav              ← placeholder garsas
│           └── mipmap-*/ic_launcher.png   ← placeholder ikona
```
