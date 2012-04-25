#!/bin/sh

if [ $# -ne 4 ]
then
  echo "Usage: `basename $0` {tagVersion} {nextVersion} {username} {password}"
  echo "Run this script from the parent project"
  exit
fi

timestamp=`date -u +%Y%m%d%H%M`
tagVersion=$1
# With this approach, p2 generation fails saying that version-timestamp is not a
# valid sustitution pattern for version.qualifier
#releaseVersion=$tagVersion-$timestamp
releaseVersion=$tagVersion.$timestamp
nextVersion=$2.qualifier

echo "tagVersion=$tagVersion"
echo "releaseVersion=$releaseVersion"
echo "nextVersion=$nextVersion"
echo "username=$3"
#echo -e "Are you sure (y/N)? \c "
#read confirm
#if [ "-"$confirm != "-y" ]; then
#  exit
#fi

export M2_HOME=/home/usuario/apache-maven-3.0.3
export M2=$M2_HOME/bin
export MAVEN_OPTS=-Dorg.apache.maven.global-settings=/home/usuario/apache-maven-3.0.3/conf/settings.xml
export PATH=$PATH:$M2

cd trunk/es.optsicom.res.res-parent

# Prepare release versions
mvn org.eclipse.tycho:tycho-versions-plugin:set-version -Dtycho.mode=maven -DnewVersion=$releaseVersion

# Perform build with the release version
mvn clean deploy

# Commit changes (preparing for tagging)
cd ..
svn ci -m "prepare release optsicom-res-$tagVersion" --username $3 --password $4

# Tag release
svn copy https://code.sidelab.es/svn/optsicomres/res/trunk/ https://code.sidelab.es/svn/optsicomres/res/tags/optsicom-res-$tagVersion -m "copy for tag optsicom-res-$tagVersion" --username $3 --password $4

# Prepare next development version
cd es.optsicom.res.res-parent
mvn org.eclipse.tycho:tycho-versions-plugin:set-version -Dtycho.mode=maven -DnewVersion=$nextVersion

# Commit changes to trunk (trunk now contains the new development version)
cd ..
svn ci -m "prepare for next development iteration" --username $3 --password $4

echo "Done!"
