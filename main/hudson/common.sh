#!/bin/bash -e

#RE_USER=
#HUDSON_MASTER_HOST=
#STORAGE_HOST=
#JNET_USER=
#JNET_STORAGE_HOST=
#STORAGE_HOST_HTTP=
#WORKSPACE=
#NOTIFICATION_SENDTO
#NOTIFICATION_FROM
#GPG_PASSPHRASE
#HUDSON_HOME

JAVAEE_VERSION=7.0
MAJOR_VERSION=4
MINOR_VERSION=0
MICRO_VERSION=1
PRODUCT_GF="glassfish"
PRODUCT_VERSION_GF=${MAJOR_VERSION}.${MINOR_VERSION}.${MICRO_VERSION}

SSH_MASTER=${RE_USER}@${HUDSON_MASTER_HOST}
SSH_STORAGE=${RE_USER}@${STORAGE_HOST}

if [ "weekly" == "${1}" ]
then
    ARCHIVE_PATH=${PRODUCT_GF}/${PRODUCT_VERSION_GF}/promoted
    ARCHIVE_STORAGE=/onestop/${ARCHIVE_PATH}/${BUILD_ID}
elif [ "nightly" == "${1}" ]
then
    MDATE=$(date +%m_%d_%Y)
    BUILD_ID=`cat /net/bat-sca.us.oracle.com/repine/export2/hudson/promote-trunk.version`
    ARCHIVE_PATH=${PRODUCT_GF}/${PRODUCT_VERSION_GF}/nightly
    ARCHIVE_STORAGE=/onestop/${ARCHIVE_PATH}/${BUILD_ID}-${MDATE}
    IPS_REPO_URL=http://localhost
    IPS_REPO_DIR=${WORKSPACE}/promorepo
    IPS_REPO_PORT=16500
    IPS_REPO_TYPE=sunos-sparc
    UC2_BUILD=2.3-b56
else
    echo "wrong argument passed, please pass either weekly or nightly as the first positional parameter to the script"
    exit 1
fi

PROMOTED_BUNDLES=${PROMOTED_URL}/artifact/bundles/
ARCHIVE_STORAGE_BUNDLES=${ARCHIVE_STORAGE}/archive/bundles
ARCHIVE_MASTER=/java/re/${ARCHIVE_STORAGE}

if [ "weekly" == "${1}" ]
then
    SCP=$SSH_STORAGE:$ARCHIVE_STORAGE_BUNDLES
    ARCHIVE_MASTER_BUNDLES=${ARCHIVE_MASTER}/archive/bundles
elif [ "nightly" == "${2}" ]
then
    SCP=$SSH_STORAGE:$ARCHIVE_STORAGE
    ARCHIVE_MASTER_BUNDLES=${ARCHIVE_MASTER}
fi

JNET_DIR=${JNET_USER}@${JNET_STORAGE_HOST}:/export/nfs/dlc/${ARCHIVE_PATH}
JNET_DIR_HTTP=http://download.java.net/${ARCHIVE_PATH}
ARCHIVE_URL=http://${STORAGE_HOST_HTTP}/${ARCHIVE_MASTER_BUNDLES}
PROMOTED_BUNDLES=${PROMOTED_URL}/artifact/bundles/
GF_WORKSPACE_URL_SSH=svn+ssh://${RE_USER}@svn.java.net/glassfish~svn
GF_WORKSPACE_URL_HTTP=https://svn.java.net/svn/glassfish~svn
UC_HOME_URL=http://${STORAGE_HOST_HTTP}/java/re/updatecenter/2.3/promoted/B56/archive/uc2/build 

init_release_version(){
    if [ ! -z $RELEASE_VERSION ] && [ ${#RELEASE_VERSION} -gt 0 ]
    then
    	printf "\n%s \n\n" "===== BUILD PARAMETER (RELEASE_VERSION) ====="
        printf "%s\n\n" "Using provided value: $RELEASE_VERSION"
    else
        RELEASE_VERSION="$PRODUCT_VERSION_GF-$BUILD_ID"
    fi
}

align_column(){
    max=$1
    char=$2
    string=$3
    stringlength=${#string}
    y=$((max-stringlength))
    while [ $y -gt 0 ]
    do
        string="$string$char"
        y=$((y-1))
    done
    echo "$string"
}

aggregated_tests_summary(){
    # Hudson rest API does not give the report
    # parsing html with xpath
    HTML_REPORT=`curl $1 2> /dev/null`
    XPATH_REQUEST="//table[@class='pane sortable']/tr[position()>1]//*[not(child::a)]/text()"
    XPATH_RESULT=`xpath -q -e "$XPATH_REQUEST" <<< $HTML_REPORT`
    XPATH_RESULT=`sed -r 's/^\s*//; s/\s*$//; /^$/d' <<< $XPATH_RESULT`

    while true
    do
        JOB_NAME=`cut -d ' ' -f1 <<< $XPATH_RESULT`
        JOB_NUMBER=`cut -d ' ' -f2 <<< $XPATH_RESULT`
        FAILED_NUMBER=`cut -d ' ' -f3 <<< $XPATH_RESULT`
        TOTAL_NUMBER=`cut -d ' ' -f4 <<< $XPATH_RESULT`
        if [ ${#JOB_NAME} -eq 0 ] || [ ${#JOB_NUMBER} -eq 0 ] || [ ${#FAILED_NUMBER} -eq 0 ] || [ ${#TOTAL_NUMBER} -eq 0 ] 
        then
            break
        else
            XPATH_RESULT=`cut -d ' ' -f5- <<< $XPATH_RESULT`
        fi

        if [ $TOTAL_NUMBER != "N/A" ] && [ $FAILED_NUMBER != "N/A" ]
        then
            PASSED_NUMBER=$((TOTAL_NUMBER-FAILED_NUMBER))
            printf "%s%s%s \n" \
                "`align_column 55 "." "$JOB_NAME ($JOB_NUMBER)"`" \
                "`align_column 12 ' ' "PASSED($PASSED_NUMBER)"`" \
                "FAILED($FAILED_NUMBER)"
            EMPTY="false"
        fi
    done

    if [ $EMPTY = "true" ]
    then
        printf "No downstream test result found."
    fi
}

scp_jnet(){
    file=$1
    simple_name=`echo $1 | \
        sed -e s@"$PRODUCT_GF-$PRODUCT_VERSION_GF-web"@web@g \
        -e s@"$JAVAEE_VERSION-$BUILD_ID-"@@g \
        -e s@"-$JAVAEE_VERSION-$BUILD_ID"@@g \
        -e s@"$BUILD_ID-"@@g \
        -e s@"-$BUILD_ID"@@g \
        -e s@"-$PRODUCT_VERSION_GF"@@g \
        -e s@"$PKG_ID-"@@g \
        -e s@"--"@"-"@g `
    ssh $SSH_MASTER "scp `echo $ARCHIVE_MASTER_BUNDLES/$file $JNET_DIR`"
    ssh $SSH_MASTER "scp `echo $ARCHIVE_MASTER_BUNDLES/$file $JNET_DIR/latest-$simple_name`"
}

promote_bundle(){
    curl $1 > $2
    scp $2 ${SCP}
    scp_jnet $2
    simple_name=`echo $i | tr -d " " | sed \
        -e s@"$PRODUCT_VERSION_GF-"@@g \
        -e s@"$BUILD_ID-"@@g \
        -e s@"--"@"-"@g`
    echo "$simple_name -> $ARCHIVE_URL/$i" >> ${1}-promotion-summary.txt
}