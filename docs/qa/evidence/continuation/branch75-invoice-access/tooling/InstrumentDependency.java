import java.nio.file.*;
import java.util.zip.*;
import org.jacoco.core.instr.Instrumenter;
import org.jacoco.core.runtime.OfflineInstrumentationAccessGenerator;
public class InstrumentDependency {
 public static void main(String[] args)throws Exception{
  var instrumenter=new Instrumenter(new OfflineInstrumentationAccessGenerator());
  int checked=0,failures=0;
  try(var zip=new ZipFile(args[0])){
   var entries=zip.entries();
   while(entries.hasMoreElements()){
    var entry=entries.nextElement();if(!entry.getName().endsWith(".class"))continue;
    checked++;
    try(var in=zip.getInputStream(entry)){instrumenter.instrument(in.readAllBytes(),entry.getName());}
    catch(Exception error){failures++;if(failures==1){System.out.println("FIRST_FAILED_CLASS="+entry.getName());error.printStackTrace(System.out);}}
   }
  }
  System.out.println("CHECKED_CLASSES="+checked+" INSTRUMENTATION_FAILURES="+failures);
  if(failures>0)System.exit(1);
 }
}
