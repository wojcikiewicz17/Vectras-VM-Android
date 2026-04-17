#!/usr/bin/env sh

#
# Copyright 2015 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

# Attempt to set APP_HOME
# Resolve links: $0 may be a link
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done
SAVED="`pwd`"
cd "`dirname \"$PRG\"`/" >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD="maximum"

warn () {
    echo "$*"
}

die () {
    echo
    echo "$*"
    echo
    exit 1
}

# OS specific support (must be 'true' or 'false').
cygwin=false
msys=false
darwin=false
nonstop=false
case "`uname`" in
  CYGWIN* )
    cygwin=true
    ;;
  Darwin* )
    darwin=true
    ;;
  MINGW* )
    msys=true
    ;;
  NONSTOP* )
    nonstop=true
    ;;
esac

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

has_meaningful_gradle_args() {
    for arg in "$@"; do
        case "$arg" in
            --version|-v|--help|-h|help|tasks|properties|projects|dependencies|dependencyInsight)
                ;;
            --*)
                ;;
            *)
                return 0
                ;;
        esac
    done
    return 1
}

resolve_property_from_gradle_properties() {
    key="$1"
    file="$APP_HOME/gradle.properties"
    if [ ! -f "$file" ]; then
        return 1
    fi
    awk -F= -v key="$key" '
        /^[[:space:]]*#/ { next }
        {
            k=$1
            gsub(/^[[:space:]]+|[[:space:]]+$/, "", k)
            if (k == key) {
                sub(/^[^=]*=/, "", $0)
                v=$0
                gsub(/^[[:space:]]+|[[:space:]]+$/, "", v)
                print v
                exit
            }
        }
    ' "$file"
}

ensure_local_properties_android_paths() {
    if ! has_meaningful_gradle_args "$@"; then
        return 0
    fi

    local_properties="$APP_HOME/local.properties"
    sdk_dir_current=""
    if [ -f "$local_properties" ]; then
        sdk_dir_current=$(awk -F= '/^[[:space:]]*sdk\.dir[[:space:]]*=/ {sub(/^[^=]*=/, "", $0); gsub(/^[[:space:]]+|[[:space:]]+$/, "", $0); print $0; exit }' "$local_properties")
    fi

    sdk_root="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
    if [ -n "$sdk_dir_current" ] && [ -d "$sdk_dir_current" ]; then
        sdk_root="$sdk_dir_current"
    fi
    if [ -z "$sdk_root" ]; then
        for candidate in \
            "$APP_HOME/.android-sdk" \
            "/workspace/android-sdk" \
            "/usr/lib/android-sdk" \
            "/opt/android-sdk" \
            "/opt/android-sdk-linux" \
            "$HOME/Android/Sdk"; do
            if [ -d "$candidate" ]; then
                sdk_root="$candidate"
                break
            fi
        done
    fi

    if [ -z "$sdk_root" ] || [ ! -d "$sdk_root" ]; then
        return 0
    fi

    escaped_sdk_root=$(printf '%s' "$sdk_root" | sed 's/\\/\\\\/g')
    ndk_version="$(resolve_property_from_gradle_properties ndk.version)"
    [ -n "$ndk_version" ] || ndk_version="$(resolve_property_from_gradle_properties NDK_VERSION)"
    if [ -n "$ndk_version" ] && [ ! -d "$sdk_root/ndk/$ndk_version" ]; then
        warn "[gradlew] NDK version from gradle.properties not found under SDK root: $sdk_root/ndk/$ndk_version"
    fi

    if [ -f "$local_properties" ]; then
        tmp_file="$(mktemp)"
        awk -v sdk_dir="$escaped_sdk_root" '
            BEGIN { sdk_written=0 }
            /^[[:space:]]*sdk\.dir[[:space:]]*=/ {
                if (!sdk_written) {
                    print "sdk.dir=" sdk_dir
                    sdk_written=1
                }
                next
            }
            /^[[:space:]]*ndk\.dir[[:space:]]*=/ { next }
            { print }
            END {
                if (!sdk_written) print "sdk.dir=" sdk_dir
            }
        ' "$local_properties" > "$tmp_file" && mv "$tmp_file" "$local_properties"
    else
        echo "sdk.dir=$escaped_sdk_root" > "$local_properties"
    fi
}

ensure_local_properties_android_paths "$@"


# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        # IBM's JDK on AIX uses strange locations for the executables
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
else
    for CANDIDATE in \
        "$HOME/.local/share/mise/installs/java/21/bin/java" \
        "$HOME/.local/share/mise/installs/java/17/bin/java" \
        "/usr/lib/jvm/java-21-openjdk-amd64/bin/java" \
        "/usr/lib/jvm/java-21-openjdk/bin/java" \
        "/usr/lib/jvm/temurin-21-jdk-amd64/bin/java" \
        "/usr/lib/jvm/java-17-openjdk-amd64/bin/java" \
        "/usr/lib/jvm/java-17-openjdk/bin/java"; do
        if [ -x "$CANDIDATE" ] ; then
            JAVACMD="$CANDIDATE"
            JAVA_HOME=$(dirname "$(dirname "$CANDIDATE")")
            export JAVA_HOME
            break
        fi
    done
    if [ -z "$JAVACMD" ] ; then
        JAVACMD="java"
    fi
    command -v "$JAVACMD" >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
fi

# Gradle 8.x fails with newer JDK bytecode levels (e.g. Java 25 / major 69).
# Auto-fallback to a local JDK 21/22 when available so ./gradlew works in CI and local dev.
JAVA_MAJOR=`"$JAVACMD" -version 2>&1 | awk -F '[\".]' '/version/ {print $2; exit}'`
if [ -n "$JAVA_MAJOR" ] && [ "$JAVA_MAJOR" -gt 22 ] 2>/dev/null ; then
    for CANDIDATE in \
        "$HOME/.local/share/mise/installs/java/22.0.2" \
        "$HOME/.local/share/mise/installs/java/21.0.2"; do
        if [ -x "$CANDIDATE/bin/java" ] ; then
            JAVA_HOME="$CANDIDATE"
            export JAVA_HOME
            JAVACMD="$JAVA_HOME/bin/java"
            break
        fi
    done
fi

# Increase the maximum file descriptors if we can.
if [ "$cygwin" = "false" -a "$darwin" = "false" -a "$nonstop" = "false" ] ; then
    MAX_FD_LIMIT=`ulimit -H -n`
    if [ $? -eq 0 ] ; then
        if [ "$MAX_FD" = "maximum" -o "$MAX_FD" = "max" ] ; then
            MAX_FD="$MAX_FD_LIMIT"
        fi
        ulimit -n $MAX_FD
        if [ $? -ne 0 ] ; then
            warn "Could not set maximum file descriptor limit: $MAX_FD"
        fi
    else
        warn "Could not query maximum file descriptor limit: $MAX_FD_LIMIT"
    fi
fi

# For Darwin, add options to specify how the application appears in the dock
if $darwin; then
    GRADLE_OPTS="$GRADLE_OPTS \"-Xdock:name=$APP_NAME\" \"-Xdock:icon=$APP_HOME/media/gradle.icns\""
fi

# For Cygwin or MSYS, switch paths to Windows format before running java
if [ "$cygwin" = "true" -o "$msys" = "true" ] ; then
    APP_HOME=`cygpath --path --mixed "$APP_HOME"`
    CLASSPATH=`cygpath --path --mixed "$CLASSPATH"`

    JAVACMD=`cygpath --unix "$JAVACMD"`

    # We build the pattern for arguments to be converted via cygpath
    ROOTDIRSRAW=`find -L / -maxdepth 1 -mindepth 1 -type d 2>/dev/null`
    SEP=""
    for dir in $ROOTDIRSRAW ; do
        ROOTDIRS="$ROOTDIRS$SEP$dir"
        SEP="|"
    done
    OURCYGPATTERN="(^($ROOTDIRS))"
    # Add a user-defined pattern to the cygpath arguments
    if [ "$GRADLE_CYGPATTERN" != "" ] ; then
        OURCYGPATTERN="$OURCYGPATTERN|($GRADLE_CYGPATTERN)"
    fi
    # Now convert the arguments - kludge to limit ourselves to /bin/sh
    i=0
    for arg in "$@" ; do
        CHECK=`echo "$arg"|egrep -c "$OURCYGPATTERN" -`
        CHECK2=`echo "$arg"|egrep -c "^-"`                                 ### Determine if an option

        if [ $CHECK -ne 0 ] && [ $CHECK2 -eq 0 ] ; then                    ### Added a condition
            eval `echo args$i`=`cygpath --path --ignore --mixed "$arg"`
        else
            eval `echo args$i`="\"$arg\""
        fi
        i=`expr $i + 1`
    done
    case $i in
        0) set -- ;;
        1) set -- "$args0" ;;
        2) set -- "$args0" "$args1" ;;
        3) set -- "$args0" "$args1" "$args2" ;;
        4) set -- "$args0" "$args1" "$args2" "$args3" ;;
        5) set -- "$args0" "$args1" "$args2" "$args3" "$args4" ;;
        6) set -- "$args0" "$args1" "$args2" "$args3" "$args4" "$args5" ;;
        7) set -- "$args0" "$args1" "$args2" "$args3" "$args4" "$args5" "$args6" ;;
        8) set -- "$args0" "$args1" "$args2" "$args3" "$args4" "$args5" "$args6" "$args7" ;;
        9) set -- "$args0" "$args1" "$args2" "$args3" "$args4" "$args5" "$args6" "$args7" "$args8" ;;
    esac
fi

# Escape application args
save () {
    for i do printf %s\\n "$i" | sed "s/'/'\\\\''/g;1s/^/'/;\$s/\$/' \\\\/" ; done
    echo " "
}
APP_ARGS=`save "$@"`

# Collect all arguments for the java command, following the shell quoting and substitution rules
eval set -- $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS "\"-Dorg.gradle.appname=$APP_BASE_NAME\"" -classpath "\"$CLASSPATH\"" org.gradle.wrapper.GradleWrapperMain "$APP_ARGS"

exec "$JAVACMD" "$@"
