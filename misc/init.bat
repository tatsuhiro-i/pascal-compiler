@echo off
cd /d %~dp0
cd ..

echo ">> �������݋֎~�t�@�C����read-only�ɕύX��..."
attrib +R src/main/java/enshud/Main.java
attrib +R src/main/java/enshud/casl/CaslSimulator.java
attrib +R src/test/java/enshud/s0/trial/TrialTest.java
attrib +R src/test/java/enshud/s1/lexer/LexerTest.java
attrib +R src/test/java/enshud/s2/parser/ParserTest.java
attrib +R src/test/java/enshud/s3/checker/CheckerTest.java
attrib +R src/test/java/enshud/s4/compiler/CompilerTest.java
attrib +R build.gradle
attrib +R settings.gradle
attrib +R data/pas/*
attrib +R data/ts/*
attrib +R data/ans/*
attrib +R data/cas/*

echo ">> git��pre-commit hook��ǉ���..."
cp misc\pre-commit .git\hooks\

echo ">> git�̃f�t�H���g�ݒ���X�V��..."
rem Search uid from .git/config
setlocal enabledelayedexpansion
for /f "tokens=*" %%a in (.git\config) do (
  set d=%%a
  for %%f in ("!d:/=" "!" "%") do (
    for /f %%n in ('echo %%f^|findstr /b /e /r "\"09B[0-9]*\""') do (
      set uid=%%~n
    )
  )
)

rem Write uid to .git/config as user.name
(
  echo [user]
  echo 	name = %uid%
  echo 	email = %uid%@localhost
) >> .git\config

echo ">> gradle�̏�������..."
call gradlew.bat cleanEclipse
call gradlew.bat eclipse

echo "   ���KD�v���W�F�N�g�͐���������������܂���"
pause
