set JAVA_HOME="V:\tools\java\jdk-25"

set new_version=1.2.3

set jsh_content=^
    Files.writeString(Path.of("README.md"), ^
        Files.readString(Path.of("README.md")).replaceAll(^
            "<brinvex-persistence.version>(.*)</brinvex-persistence.version>", ^
            "<brinvex-persistence.version>%%s</brinvex-persistence.version>".formatted(System.getenv("new_version"))), ^
    StandardOpenOption.TRUNCATE_EXISTING);

echo %jsh_content% | %JAVA_HOME%\bin\jshell -

call mvnw clean package
call mvnw versions:set -DnewVersion=%new_version%
call mvnw versions:commit
call mvnw clean deploy -T 1 -DskipTests

REM Commit and push