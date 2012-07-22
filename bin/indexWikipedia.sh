#/bin/sh
if  [ -z $JAVA_HOME ] ; then
 export JAVA_HOME="/Library/Java/Home"
fi

for jar in ../target/*.jar;do
 export CLASSPATH=$CLASSPATH:$jar
done
for jar in ../target/dependency/*.jar;do
 if [ $jar = "../target/dependency/jcl-over-slf4j-1.6.1.jar" ]; then
     echo "Skipping $jar"
 else
     export CLASSPATH=$CLASSPATH:$jar
 fi
done

export MEM_ARGS="-Xms512m -Xmx512m"

RUN_CMD="$JAVA_HOME/bin/java $MEM_ARGS -cp $CLASSPATH com.tamingtext.qa.WikipediaWexIndexer $@"
echo $RUN_CMD
exec $RUN_CMD

