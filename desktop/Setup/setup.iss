; ============================================================================
;  MEHRA 1.1 — MehraSetup.iss  (Inno Setup 6, Unicode/UTF-8)
;  Luxury Persian (RTL) installer for the MEHRA Windows client.
;  Brand: Milad Yaghoobi — www.meelano.ir — 09120219593
;  Features: branded welcome/finish, Vazirmatn font, SQL Server config page,
;            live connection test, table creation (db\schema.sql), progress %,
;            smart error detection (disk space, Windows version, SQL errors).
; ============================================================================

[Setup]
AppId={{7C4E9A21-3B6D-4F58-9A01-5E2D8B7C6F40}}
AppName=مهرآ
AppVersion=1.1.1
AppVerName=مهرآ ۱.۱.۱ — ویندوز
AppPublisher=Milad Yaghoobi — میلاد یعقوبی
AppPublisherURL=http://www.meelano.ir
AppSupportURL=http://www.meelano.ir
AppUpdatesURL=http://www.meelano.ir
AppCopyright=© 2026 Milad Yaghoobi (میلاد یعقوبی) — www.meelano.ir — 09120219593
DefaultDirName={autopf}\Mehra
DefaultGroupName=مهرآ
DisableProgramGroupPage=yes
PrivilegesRequired=admin
CloseApplications=yes
RestartApplications=yes
OutputDir=..\..\setup-out
OutputBaseFilename=MehraSetup
SetupIconFile=..\Mehra.Desktop\Assets\mehra.ico
WizardStyle=modern
WizardImageFile=art_side.bmp
WizardSmallImageFile=art_top.bmp
Compression=lzma2/ultra64
SolidCompression=yes
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
UninstallDisplayIcon={app}\Mehra.exe
UninstallDisplayName=مهرآ
VersionInfoVersion=1.1.1.0
VersionInfoCompany=Milad Yaghoobi — میلاد یعقوبی — http://www.meelano.ir — 09120219593
VersionInfoDescription=سامانهٔ مدیریت مهمان‌پذیر فرهنگان اهواز — نسخهٔ ویندوز
VersionInfoCopyright=© 2026 میلاد یعقوبی (Milad Yaghoobi) — www.meelano.ir — 09120219593
DisableWelcomePage=no

[Languages]
Name: "farsi"; MessagesFile: "Farsi.isl"

[Messages]
ButtonBack=«قبلی»
ButtonNext=«بعدی»
ButtonCancel=«انصراف»
ButtonWizardBrowse=«انتخاب…»
ButtonFinish=«پایان»
ButtonInstall=«نصب»
ButtonYes=«بله»
ButtonNo=«خیر»
ButtonOK=«تأیید»
WelcomeLabel1=به نصب‌گر مهرآ خوش آمدید
WelcomeLabel2=سامانهٔ مدیریت مهمان‌پذیر فرهنگان اهواز — نسخهٔ ویندوز ۱.۱\n\nطراح و توسعه‌دهنده: میلاد یعقوبی (Milad Yaghoobi)\nwww.meelano.ir — 09120219593
WizardSelectDir=انتخاب مقصد نصب
SelectDirLabel=برنامه در این پوشه نصب می‌شود:
SelectDirBrowseLabel=انتخاب پوشه…
DiskSpaceGBLabel=فضای خالی:
DiskSpaceMBLabel=مگابایت
WizardSelectTasks=امکانات نصب
TasksLabel=لطفاً گزینهایی که می‌خواهی در هنگام نصب انجام شود:
WizardReady=آمادهٔ نصب
ReadyLabel=برای شروع نصب روی «نصب» کلیک کنید.
WizardInstalling=در حال نصب مهرآ…
StatusExtractFiles=در حال استخراج فایل‌ها…
StatusCreateDirs=در حال ایجاد پوشه‌ها…
StatusInstalling=در حال استخراج فایل‌ها…
FinishedHeadingLabel=نصب با موفقیت انجام شد!
FinishedLabel=مهرآ اکنون آمادهٔ استفاده است.\n\nمیلاد یعقوبی (Milad Yaghoobi) — www.meelano.ir — 09120219593
ClickFinish=برای خروج از نصب‌گر روی «پایان» کلیک کنید.
SetupWindowTitle=مهرآ ۱.۱.۱ — نصب‌گر

[Components]
Name: "app"; Description: "فایل‌های برنامه (Mehra.exe)"; Types: full compact custom; Flags: fixed
Name: "localdb"; Description: "SQL Server LocalDB 2022 همراه (حدود ۵۵ مگابایت) — موتور پایگاه دادهٔ سبک؛ ثبت واقعی داده روی همین PC بدون نصب جداگانه"; Types: full custom
Name: "schema"; Description: "فایل اسکریپت پایگاه داده (db‌schema.sql) — برای ساخت جداول روی SQL Server"; Types: full
Name: "fonts"; Description: "فونت‌های وزیرمتن (6 وزن) — نصب دائمی در سیستم"; Types: full

[Tasks]
Name: "desktopicon"; Description: "ایجاد میان‌بر روی دسکتاپ"; GroupDescription: "گزینه‌های اضافی:"
Name: "runapp"; Description: "اجرای برنامه پس از پایان نصب"; GroupDescription: "گزینه‌های اضافی:"
Name: "createdb"; Description: "ایجاد/به‌روزرانی جداول پایگاه داده در SQL Server هنگام نصب (اجرای db\schema.sql)"; GroupDescription: "پایگاه دادهٔ SQL Server"

[Files]
Source: "..\publish\Mehra.exe"; DestDir: "{app}"; Flags: ignoreversion restartreplace; Components: app
Source: "redist\SqlLocalDB.msi"; DestDir: "{tmp}"; Flags: ignoreversion deleteafterinstall; Components: localdb
Source: "..\Mehra.Desktop\db\schema.sql"; DestDir: "{app}\db"; Flags: ignoreversion; Components: schema
Source: "vazirmatn_regular.ttf"; DestDir: "{tmp}"; Flags: ignoreversion deleteafterinstall; Components: fonts
Source: "vazirmatn_medium.ttf"; DestDir: "{tmp}"; Flags: ignoreversion deleteafterinstall; Components: fonts
Source: "vazirmatn_semibold.ttf"; DestDir: "{tmp}"; Flags: ignoreversion deleteafterinstall; Components: fonts
Source: "vazirmatn_bold.ttf"; DestDir: "{tmp}"; Flags: ignoreversion deleteafterinstall; Components: fonts
Source: "vazirmatn_extrabold.ttf"; DestDir: "{tmp}"; Flags: ignoreversion deleteafterinstall; Components: fonts
Source: "vazirmatn_black.ttf"; DestDir: "{tmp}"; Flags: ignoreversion deleteafterinstall; Components: fonts
Source: "vazirmatn_regular.ttf"; DestDir: "{fonts}"; Flags: ignoreversion restartreplace; Check: IsAdminLoggedOnSafe; Components: fonts
Source: "vazirmatn_regular.ttf"; DestDir: "{userfonts}"; Flags: ignoreversion restartreplace; Check: not IsAdminLoggedOnSafe; Components: fonts
Source: "vazirmatn_medium.ttf"; DestDir: "{fonts}"; Flags: ignoreversion restartreplace; Check: IsAdminLoggedOnSafe; Components: fonts
Source: "vazirmatn_medium.ttf"; DestDir: "{userfonts}"; Flags: ignoreversion restartreplace; Check: not IsAdminLoggedOnSafe; Components: fonts
Source: "vazirmatn_semibold.ttf"; DestDir: "{fonts}"; Flags: ignoreversion restartreplace; Check: IsAdminLoggedOnSafe; Components: fonts
Source: "vazirmatn_semibold.ttf"; DestDir: "{userfonts}"; Flags: ignoreversion restartreplace; Check: not IsAdminLoggedOnSafe; Components: fonts
Source: "vazirmatn_bold.ttf"; DestDir: "{fonts}"; Flags: ignoreversion restartreplace; Check: IsAdminLoggedOnSafe; Components: fonts
Source: "vazirmatn_bold.ttf"; DestDir: "{userfonts}"; Flags: ignoreversion restartreplace; Check: not IsAdminLoggedOnSafe; Components: fonts
Source: "vazirmatn_extrabold.ttf"; DestDir: "{fonts}"; Flags: ignoreversion restartreplace; Check: IsAdminLoggedOnSafe; Components: fonts
Source: "vazirmatn_extrabold.ttf"; DestDir: "{userfonts}"; Flags: ignoreversion restartreplace; Check: not IsAdminLoggedOnSafe; Components: fonts
Source: "vazirmatn_black.ttf"; DestDir: "{fonts}"; Flags: ignoreversion restartreplace; Check: IsAdminLoggedOnSafe; Components: fonts
Source: "vazirmatn_black.ttf"; DestDir: "{userfonts}"; Flags: ignoreversion restartreplace; Check: not IsAdminLoggedOnSafe; Components: fonts

[Icons]
Name: "{group}\مهرآ"; Filename: "{app}\Mehra.exe"; IconFilename: "{app}\Mehra.exe"
Name: "{group}\حذف برنامه"; Filename: "{uninstallexe}"
Name: "{userdesktop}\مهرآ"; Filename: "{app}\Mehra.exe"; IconFilename: "{app}\Mehra.exe"; Tasks: desktopicon

[Run]
Filename: "{app}\Mehra.exe"; Description: "اجرای مهرآ"; Flags: nowait postinstall skipifsilent; Tasks: runapp

[Code]
var
  DbPage: TInputQueryWizardPage;
  OptPage: TInputOptionWizardPage;
  PctLabel: TLabel;
  LocalDbOk: Boolean;

function AddFontResourceExW(lpszFilename: string; fl: LongWord; pdv: LongWord): LongWord;
  external 'AddFontResourceExW@gdi32.dll stdcall delayload';
function IsAdminLoggedOn: Boolean;
  external 'IsAdminLoggedOn@shell32.dll stdcall delayload';

{ Never let a shell32 hiccup abort the whole setup: on any failure assume admin
  (system-wide fonts dir); the per-user fonts fallback is only an optimisation. }
function IsAdminLoggedOnSafe: Boolean;
begin
  try
    Result := IsAdminLoggedOn;
  except
    Result := True;
  end;
end;
function AddFontResourceW(lpszFilename: string): LongWord;
  external 'AddFontResourceW@gdi32.dll stdcall delayload';
function GetDiskFreeSpaceExW(lpDirectoryName: string; var lpFreeBytesAvailableToCaller: Int64;
  var lpTotalNumberOfBytes: Int64; var lpTotalNumberOfFreeBytes: Int64): Boolean;
  external 'GetDiskFreeSpaceExW@kernel32.dll stdcall delayload';

procedure RegisterFonts;
begin
  AddFontResourceExW(ExpandConstant('{tmp}\\vazirmatn_regular.ttf'), $10, 0);
  AddFontResourceExW(ExpandConstant('{tmp}\\vazirmatn_medium.ttf'), $10, 0);
  AddFontResourceExW(ExpandConstant('{tmp}\\vazirmatn_semibold.ttf'), $10, 0);
  AddFontResourceExW(ExpandConstant('{tmp}\\vazirmatn_bold.ttf'), $10, 0);
  AddFontResourceExW(ExpandConstant('{tmp}\\vazirmatn_extrabold.ttf'), $10, 0);
  AddFontResourceExW(ExpandConstant('{tmp}\\vazirmatn_black.ttf'), $10, 0);
end;

function ReplaceAll(s, from, to_: string): string;
var
  i, fl: Integer;
begin
  Result := ''; fl := Length(from); i := 1;
  while i <= Length(s) do
  begin
    if (i + fl - 1 <= Length(s)) and (Copy(s, i, fl) = from) then
    begin
      Result := Result + to_;
      i := i + fl;
    end
    else
    begin
      Result := Result + Copy(s, i, 1);
      i := i + 1;
    end;
  end;
end;

function EscBs(s: string): string;
var
  i: Integer;
  c: string;
begin
  Result := '';
  for i := 1 to Length(s) do
  begin
    c := Copy(s, i, 1);
    if c = '\' then Result := Result + '\\' else Result := Result + c;
  end;
end;

function MakeConnStr(provider, server, db: string; winAuth: Boolean; user, pass: string): string;
begin
  if winAuth then
    Result := 'Provider=' + provider + ';Data Source=' + server + ';Initial Catalog=' + db +
              ';Integrated Security=SSPI;Encrypt=Optional;TrustServerCertificate=True;'
  else
    Result := 'Provider=' + provider + ';Data Source=' + server + ';Initial Catalog=' + db +
              ';User Id=' + user + ';Password=' + pass + ';Encrypt=Optional;TrustServerCertificate=True;';
end;

function TryProviderOpen(provider, server, db: string; winAuth: Boolean; user, pass: string; var Conn: Variant): Boolean;
var
  ConnStr: string;
begin
  Result := False;
  try
    Conn := CreateOleObject(provider);
    Conn.ConnectionTimeout := 8;
    Conn.CommandTimeout := 60;
    ConnStr := MakeConnStr(provider, server, db, winAuth, user, pass);
    Conn.Open(ConnStr);
    Result := True;
  except
    Result := False;
  end;
end;

function TestSql(server, db: string; winAuth: Boolean; user, pass: string; var Err: string): Boolean;
var
  Providers: array[0..3] of string;
  i: Integer;
  Conn: Variant;
begin
  Result := False; Err := '';
  Providers[0] := 'MSOLEDBSQL'; Providers[1] := 'MSOLEDBSQL.1';
  Providers[2] := 'SQLOLEDB.1'; Providers[3] := 'SQLOLEDB';
  for i := 0 to 3 do
  begin
    if TryProviderOpen(Providers[i], server, db, winAuth, user, pass, Conn) then
    begin
      try
        Conn.Execute('SELECT 1');
        Conn.Close;
        Result := True;
        Exit;
      except
        Err := GetExceptionMessage;
      end;
    end;
  end;
  if Err = '' then Err := 'اتصال به SQL Server برقرار نشد:';
end;

function RunSchema(server, db: string; winAuth: Boolean; user, pass: string; schemaFile: string; var Err: string): Boolean;
var
  Providers: array[0..3] of string;
  i: Integer;
  Conn: Variant;
  Lines: TArrayOfString;
  Batches: TStringList;
  j: Integer;
  Cur: string;
begin
  Result := False; Err := '';
  Providers[0] := 'MSOLEDBSQL'; Providers[1] := 'MSOLEDBSQL.1';
  Providers[2] := 'SQLOLEDB.1'; Providers[3] := 'SQLOLEDB';
  Batches := TStringList.Create;
  try
    if not LoadStringsFromFile(schemaFile, Lines) then
    begin
      Err := 'فایل schema.sql پیدا نشد.';
      Exit;
    end;
    for j := 0 to Length(Lines) - 1 do
      Lines[j] := ReplaceAll(Lines[j], 'Mehra', db);
    Cur := '';
    for j := 0 to Length(Lines) - 1 do
    begin
      if UpperCase(Trim(Lines[j])) = 'GO' then
      begin
        Batches.Add(Cur);
        Cur := '';
      end
      else
        Cur := Cur + Lines[j] + #13#10;
    end;
    if Trim(Cur) <> '' then Batches.Add(Cur);
    for i := 0 to 3 do
    begin
      if TryProviderOpen(Providers[i], server, 'master', winAuth, user, pass, Conn) then
      begin
        try
          for j := 0 to Batches.Count - 1 do
          begin
            if Trim(Batches[j]) <> '' then
              Conn.Execute(Batches[j]);
          end;
          Conn.Close;
          Result := True;
          Exit;
        except
          Err := GetExceptionMessage;
        end;
      end;
    end;
  finally
    Batches.Free;
  end;
end;

// Locate the newest sqllocaldb.exe under Program Files\Microsoft SQL Server\<ver>\Tools\Binn
function FindSqlLocalDbExe: string;
var
  DirPat, ExePat, BestDir: string;
  SR: TFindRec;
begin
  Result := '';
  BestDir := '';
  DirPat := ExpandConstant('{pf}\Microsoft SQL Server\*');
  // Inno Pascal FindFirst takes (Path, var FindRec) and returns dirs too
  if FindFirst(DirPat, SR) then
  begin
    try
      repeat
        if (SR.Name <> '.') and (SR.Name <> '..') and
           ((SR.Attributes and FILE_ATTRIBUTE_DIRECTORY) <> 0) then
        begin
          if (BestDir = '') or (StrToIntDef(SR.Name, 0) > StrToIntDef(BestDir, 0)) then
            BestDir := SR.Name;
        end;
      until not FindNext(SR);
    finally
      FindClose(SR);
    end;
  end;
  if BestDir <> '' then
  begin
    ExePat := ExpandConstant('{pf}\Microsoft SQL Server\' + BestDir + '\Tools\Binn\sqllocaldb.exe');
    if FileExists(ExePat) then Result := ExePat;
  end;
end;

// Silently install the bundled SqlLocalDB.msi (MS license accepted via
// IACCEPTSQLLOCALDBLICENSETERMS), then create+start the MSSQLLocalDB instance.
// Never aborts the setup: on failure the caller warns and the app can still
// use any other SQL Server on the network.
function InstallLocalDb(var Err: string): Boolean;
var
  Msi, ExePath: string;
  R: Integer;
begin
  Result := False;
  Err := '';
  ExePath := FindSqlLocalDbExe;
  if ExePath <> '' then
  begin
    Result := True;
    Exit;
  end;
  Msi := ExpandConstant('{tmp}\SqlLocalDB.msi');
  if not FileExists(Msi) then
  begin
    Err := 'فایل SqlLocalDB.msi در فایل نصب یافت نشد.';
    Exit;
  end;
  if not Exec('msiexec.exe', '/i "' + Msi + '" /qn IACCEPTSQLLOCALDBLICENSETERMS=YES REBOOT=ReallySuppress', '', SW_HIDE, ewWaitUntilTerminated, R) then
  begin
    Err := 'اجرای نصب‌کنندهٔ LocalDB ممکن نشد (کد ' + IntToStr(R) + ').';
    Exit;
  end;
  if (R <> 0) and (R <> 3010) then
  begin
    Err := 'msiexec کد خروج ' + IntToStr(R) + ' داد.';
    Exit;
  end;
  ExePath := FindSqlLocalDbExe;
  if ExePath = '' then
  begin
    Err := 'پس از نصب، sqllocaldb.exe پیدا نشد.';
    Exit;
  end;
  Exec(ExePath, 'create MSSQLLocalDB -s', '', SW_HIDE, ewWaitUntilTerminated, R);
  Exec(ExePath, 'start MSSQLLocalDB', '', SW_HIDE, ewWaitUntilTerminated, R);
  Result := True;
end;
procedure KillRunningApp;
var
  R: Integer;
begin
  { If the app is running, its exe is locked -> sharing violation (error 32). }
  Exec('taskkill', '/IM Mehra.exe /F /T', '', SW_HIDE, ewWaitUntilTerminated, R);
end;

procedure InstallFonts;
var
  Names: array[0..5] of string;
  i: Integer;
  P: string;
begin
  Names[0] := 'vazirmatn_regular.ttf';
  Names[1] := 'vazirmatn_medium.ttf';
  Names[2] := 'vazirmatn_semibold.ttf';
  Names[3] := 'vazirmatn_bold.ttf';
  Names[4] := 'vazirmatn_extrabold.ttf';
  Names[5] := 'vazirmatn_black.ttf';
  for i := 0 to 5 do
  begin
    P := ExpandConstant('{fonts}\\' + Names[i]);
    if not FileExists(P) then P := ExpandConstant('{userfonts}\\' + Names[i]);
    if FileExists(P) then AddFontResourceW(P);
  end;
end;

procedure WriteAppSettings(server, db: string; sqlAuth: Boolean; user: string);
var
  Path, Json, Srv, Usr: string;
  Arr: TArrayOfString;
begin
  Path := ExpandConstant('{userappdata}\\mehra\\settings.json');
  ForceDirectories(ExtractFileDir(Path));
  Srv := EscBs(server);
  Usr := EscBs(user);
  if sqlAuth then
    Json := '{\"Mode\":\"sql\",\"ServerUrl\":\"\",\"SqlServer\":\"' + Srv + '\",\"SqlDb\":\"' + db + '\",\"SqlAuth\":\"sql\",\"SqlUser\":\"' + Usr + '\"}'
  else
    Json := '{\"Mode\":\"sql\",\"ServerUrl\":\"\",\"SqlServer\":\"' + Srv + '\",\"SqlDb\":\"' + db + '\",\"SqlAuth\":\"win\",\"SqlUser\":\"\\"\"}';
  SetLength(Arr, 1);
  Arr[0] := Json;
  SaveStringsToUTF8FileWithoutBOM(Path, Arr, False);
end;

function NextButtonClick(CurPageID: Integer): Boolean;
var
  Err: string;
  winAuth: Boolean;
begin
  Result := True;
  if CurPageID = wpSelectComponents then
  begin
    // the bundled LocalDB msi needs room to extract (~900 MB free recommended)
    if IsComponentSelected('localdb') then
    begin
      if GetDiskFreeSpaceExW(ExpandConstant('{sd}'), FreeB, TotalB, Dummy) then
      begin
        if FreeB < 900 * 1024 * 1024 then
        begin
          MsgBox('برای نصب SQL Server LocalDB همراه حدود ۹۰۰ مگابایت فضای خالی دیسک لازم است.', mbError, MB_OK);
          Result := False;
          Exit;
        end;
      end;
    end;
  end;
  if CurPageID = DbPage.ID then
  begin
    if Trim(DbPage.Values[0]) = '' then
    begin
      MsgBox('نام سرور SQL Server را وارد کنید.', mbError, MB_OK);
      Result := False;
      Exit;
    end;
    if Trim(DbPage.Values[1]) = '' then
    begin
      MsgBox('نام پایگاه داده را وارد کنید.', mbError, MB_OK);
      Result := False;
      Exit;
    end;
    winAuth := OptPage.Values[0];
    // If the bundled LocalDB is being installed by THIS setup, it does not exist
    // yet at page time — skip the live test and let ssPostInstall create everything.
    if OptPage.Values[1] and not (IsComponentSelected('localdb') and
       ((Trim(DbPage.Values[0]) = '') or (Trim(DbPage.Values[0]) = '(localdb)\MSSQLLocalDB'))) then
    begin
      if not TestSql(DbPage.Values[0], 'master', winAuth, DbPage.Values[2], DbPage.Values[3], Err) then
      begin
        MsgBox('اتصال به SQL Server برقرار نشد:' + #13#10#13#10 + Err + #13#10#13#10 + 'نام سرور/نمونه (instance)، وضع سرویس SQL Server و شبکه را بررسی کنید.', mbError, MB_OK);
        Result := False;
        Exit;
      end;
    end;
  end;
end;

procedure CurInstallProgressChanged(CurProgress, MaxProgress: Integer);
begin
  try
    if MaxProgress > 0 then
      PctLabel.Caption := IntToStr(MulDiv(CurProgress, 100, MaxProgress)) + ' ٪';
  except
  end;
end;

procedure CurStepChanged(CurStep: TSetupStep);
var
  Err: string;
  winAuth: Boolean;
  SchemaPath: string;
begin
  if CurStep = ssInstall then
  begin
    KillRunningApp;
    PctLabel.Visible := True;
    PctLabel.Caption := '0 ٪';
  end;
  if CurStep = ssPostInstall then
  begin
    InstallFonts;
    LocalDbOk := False;
    if IsComponentSelected('localdb') then
    begin
      if InstallLocalDb(Err) then
      begin
        LocalDbOk := True;
        if Trim(DbPage.Values[0]) = '' then DbPage.Values[0] := '(localdb)\MSSQLLocalDB';
        OptPage.Values[0] := True; // LocalDB همراه uses Windows auth
        Log('MEHRA: bundled LocalDB ready at (localdb)\MSSQLLocalDB');
      end
      else
      begin
        MsgBox('نصب SQL Server LocalDB همراه ناموفق بود:' + #13#10#13#10 + Err + #13#10#13#10 +
               'می‌توانید بعداً آن را دستی نصب کنید یا به SQL Server موجود در شبکه متصل شوید.', mbError, MB_OK);
      end;
    end;
    if OptPage.Values[1] and (Trim(DbPage.Values[0]) <> '') then
    begin
      winAuth := OptPage.Values[0];
      SchemaPath := ExpandConstant('{app}\\db\\schema.sql');
      if RunSchema(DbPage.Values[0], DbPage.Values[1], winAuth, DbPage.Values[2], DbPage.Values[3],
                   SchemaPath, Err) then
      begin
        WriteAppSettings(DbPage.Values[0], DbPage.Values[1], not winAuth, DbPage.Values[2]);
        Log('MEHRA: schema created on ' + DbPage.Values[0]);
      end
      else
      begin
        MsgBox('اجرای اسکریپت ساخت جداول ناموفق بود:' + #13#10#13#10 + Err + #13#10#13#10 + 'برنامه بعداً می‌تواند از بخش تنظیمات دوباره تلاش کند («راه‌اندازی جداول پایگاه داده»).', mbError, MB_OK);
      end;
    end
    else if LocalDbOk then
    begin
      // LocalDB is installed: pre-fill the app settings so first launch connects out of the box.
      WriteAppSettings(DbPage.Values[0], DbPage.Values[1], False, '');
    end;
  end;
  if CurStep = ssDone then
  begin
    PctLabel.Visible := False;
  end;
end;

function InitializeSetup: Boolean;
var
  FreeB, TotalB, Dummy: Int64;
begin
  Result := True;
  if GetWindowsVersion < $0A004246 then
  begin
    MsgBox('نسخهٔ ویندوز شما باید Windows 10 نسخهٔ 1803 (build 17134) یا جدیدتر باشد.', mbError, MB_OK);
    Result := False;
    Exit;
  end;
  if not GetDiskFreeSpaceExW(ExpandConstant('{sd}'), FreeB, TotalB, Dummy) then
    FreeB := MaxInt;
  if FreeB < 300 * 1024 * 1024 then
  begin
    MsgBox('فضای خالی دیسک کافی نیست (حدود 300 مگابایت لازم است).', mbError, MB_OK);
    Result := False;
    Exit;
  end;
end;

procedure InitializeWizard;
begin
  RegisterFonts;
  try
    WizardForm.Font.Name := 'Vazirmatn';
  except
  end;
  if not IsAdminLoggedOnSafe then
    WizardForm.DirEdit.Text := ExpandConstant('{localappdata}\Mehra');
  OptPage := CreateInputOptionPage(wpSelectTasks,
    'پایگاه دادهٔ SQL Server', 'اگر «SQL Server LocalDB همراه» را در صفحهٔ مؤلفه‌ها انتخاب کرده‌اید، موتور پایگاه داده همراه نصب ساخته می‌شود و جداول به‌صورت خودکار از db\\schema.sql ایجاد می‌گردند؛ در غیر این صورت به هر SQL Server موجود (2008 به بالا) متصل می‌شود.', 'توضیح: این بخش اختیاری است و به‌صورت خودکار از db\\schema.sql ساخته می‌شود.', False, False);
  OptPage.Add('احراز هویت ویندوز (Windows Authentication)');
  OptPage.Values[0] := True;
  OptPage.Add('ایجاد/به‌روزانی جداول پایگاه داده در SQL Server هنگام نصب');
  OptPage.Values[1] := True;

  DbPage := CreateInputQueryPage(wpSelectTasks,
    'تنظیمات اتصال SQL Server', 'نام سرور (نمونه): .\\SQLEXPRESS یا (localdb)\\MSSQLLocalDB و نام پایگاه داده.', 'این مقادیر بعداً در برنامه‌ٔ برای ورود به پایگاه داده استفاده می‌شود و اگر پایگاه داده وجود ندارد، به‌صورت خودکار ساخته می‌شود.');
  DbPage.Add('سرور SQL Server:', False);
  DbPage.Values[0] := '(localdb)\MSSQLLocalDB';
  DbPage.Add('نام پایگاه داده:', False);
  DbPage.Values[1] := 'Mehra';
  DbPage.Add('نام کاربری SQL (در حالت SQL):', False);
  DbPage.Values[2] := 'sa';
  DbPage.Add('رمز عبور SQL:', True);
  DbPage.Values[3] := '';

  PctLabel := TLabel.Create(WizardForm);
  with PctLabel do
  begin
    Parent := WizardForm.ProgressGauge.Parent;
    Font.Name := 'Vazirmatn';
    Font.Size := 12;
    Font.Style := [fsBold];
    Font.Color := $00AED6E8;
    AutoSize := False;
    Width := 90;
    Height := 22;
    Left := WizardForm.ProgressGauge.Left + WizardForm.ProgressGauge.Width + 10;
    Top := WizardForm.ProgressGauge.Top - 3;
    Alignment := taCenter;
    Caption := '0 ٪';
    Visible := False;
    Transparent := True;
  end;

end;
