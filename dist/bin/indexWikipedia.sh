#/bin/sh
if  [ -z $JAVA_HOME ] ; then
 export JAVA_HOME="/Library/Java/Home"
fi

for jar in ../lib/*.jar;do
 export CLASSPATH=$CLASSPATH:$jar
done
for jar in ../lib/dependency/*.jar;do
 export CLASSPATH=$CLASSPATH:$jar
done

export MEM_ARGS="-Xms512m -Xmx512m"

RUN_CMD="$JAVA_HOME/bin/java $MEM_ARGS -cp $CLASSPATH com.tamingtext.qa.WikipediaWexIndexer $@"
echo $RUN_CMD
exec $RUN_CMD

