CLASSPATH=.
for i in `ls lib/*.jar`;
do
CLASSPATH=$CLASSPATH:$i
done
exec java -classpath $CLASSPATH com.deem.zkui.Main
