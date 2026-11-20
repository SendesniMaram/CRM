package com.crm.payroll;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
/**
 * H2-only copies: separates MySQL multi-action ALTER and expresses ADD INDEX as CREATE INDEX.
 * All migrations remain selected. Originals and production Flyway checksums are untouched.
 * A successful adapted H2 migration does not validate MySQL syntax or production checksums.
 */
final class H2MigrationCopies {
 private H2MigrationCopies() {}
 static String location() throws IOException {
  Path root=Path.of("target","lot3i-h2-migration-copies");Files.createDirectories(root);
  Path copy=Files.createTempDirectory(root,"run-").toAbsolutePath();
  for(var resource:new PathMatchingResourcePatternResolver().getResources("classpath:db/migration/*.sql")){
   String original;
   try(var input=resource.getInputStream()){original=new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}
   Files.writeString(copy.resolve(Objects.requireNonNull(resource.getFilename())),adapt(original));
  }
  return "filesystem:"+copy;
 }
 static String adapt(String original) {
  String withoutBom=original.startsWith("\uFEFF")?original.substring(1):original;
  String sql=withoutBom.replaceAll("(?m)^\\s*--[^\\r\\n]*","");
  StringBuilder out=new StringBuilder();
  for(String statement:split(sql,';')){
   String text=statement.trim();if(text.isEmpty())continue;
   var alter=Pattern.compile("(?is)^ALTER\\s+TABLE\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s+(.+)$").matcher(text);
   if(!alter.matches()){out.append(text).append(";\n");continue;}
   for(String action:split(alter.group(2),',')){
    var index=Pattern.compile("(?is)^ADD\\s+INDEX\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*(\\(.+\\))$").matcher(action.trim());
    if(index.matches())out.append("CREATE INDEX ").append(index.group(1)).append(" ON ").append(alter.group(1)).append(index.group(2));
    else out.append("ALTER TABLE ").append(alter.group(1)).append(' ').append(action.trim());
    out.append(";\n");
   }
  }
  return out.toString();
 }
 private static List<String> split(String text,char separator) {
  List<String> result=new ArrayList<>();int depth=0,start=0;char quote=0;
  for(int n=0;n<text.length();n++){char c=text.charAt(n);
   if(quote!=0){if(c=='\\'){n++;continue;}if(c==quote)quote=0;continue;}
   if(c=='\''||c=='"'||c==(char)96){quote=c;continue;}
   if(c=='(')depth++;else if(c==')')depth--;
   else if(c==separator&&depth==0){result.add(text.substring(start,n));start=n+1;}
  }
  result.add(text.substring(start));return result;
 }
}
