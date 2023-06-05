import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Array;
import java.util.ArrayList;
import java.util.List;

public class SQLtoFunction {

    private static String filePath = "D:/docs/codes/poc/files";
    private static String injectName = "common";
    public static void main(String[] args) throws Exception {

//        String sql = "select equipment_type_name as name, equipment_type_sn as sn , e_type as type , age as age from equipment_type where previous_equipment_type_sn  != -1 order by equipment_type_sn ";
//        String sql = "select max(age) from shovel_daily_plan sdp where now() between plan_start_time and plan_end_time ";
        String sql = "select count(1) from shovel_daily_plan sdp where now() between plan_start_time and plan_end_time";
        List<String> fields = fetchField(sql);




        generateInterface(fields);
        generateController(fields);
        generateInterfaceFunction(fields);
        generateService(fields);
        generateQuery(sql,fields);

    }

    public static String getFieldName(String f){
        String name = "";
        for(int i = 0 ; i < f.length() ; i++){
            if(  !((f.charAt(i) <= 'Z' && f.charAt(i) >= 'A') || (f.charAt(i) <= 'z' && f.charAt(i) >= 'a')) ){
                break;
            }
            name += f.charAt(i);
        }
        return name;
    }

    public static List<String> fetchField(String sql){
        String fieldsString[] = sql.substring(0,sql.indexOf("from")).replaceAll("select","").split(" , ");
        List<String> fields = new ArrayList<String>();

        if( fieldsString.length == 1 && !fieldsString[0].contains("as")){
            fields.add(getFieldName(fieldsString[0].replaceAll(" ","")));
            return fields;
        }

        for(String s : fieldsString){
            String[] fs = s.split("as");
            String f = fs[0].replaceAll(" ","");
            String name = fs[1].replaceAll(" ","");
            fields.add(name);
        }

        return fields;
    }

    public static String generateUpper(String s){
        return s.substring(0,1).toUpperCase() + s.substring(1);
    }

    public static void generateController(List<String> fields) throws Exception{
        String fileName = generateUpper(fields.get(0)) + (fields.size() > 1 ? generateUpper(fields.get(1))  : "");
        System.out.println("\n");
        System.out.println("//" + fileName + " ");
        System.out.println("@RequestMapping(value = \"/get"+ fileName + "\")\n" +
                "public List<"+ fileName + "Dto> get"+ fileName + "(){\n" +
                "    return " + injectName + "Service.get"+ fileName + "();\n" +
                "}");
    }

    public static void generateInterfaceFunction(List<String> fields) throws Exception{
        String fileName = generateUpper(fields.get(0)) + (fields.size() > 1 ? generateUpper(fields.get(1))  : "");
        System.out.println("\n");
        System.out.println("//" + fileName + " interface");
        System.out.println("public List<"+ fileName + "Dto> get"+ fileName + "();" );
    }


    public static void generateService(List<String> fields) throws Exception{
        String fileName = generateUpper(fields.get(0)) + (fields.size() > 1 ? generateUpper(fields.get(1))  : "");
        System.out.println("\n");

        System.out.println("//" + fileName + " service");
        System.out.println("@Override\n" +
                "public List<"+ fileName + "Dto> get"+ fileName + "() {\n" +
                "     return " + injectName + "Respository.get"+ fileName + "();\n" +
                "}");
    }


    public static void generateQuery(String sql , List<String> fields) throws Exception{
        String fileName = generateUpper(fields.get(0)) + (fields.size() > 1 ? generateUpper(fields.get(1))  : "");
        System.out.println("\n");

        System.out.println("//" + fileName + " query");
        System.out.println("@Query(value = \"" + sql + "\", nativeQuery = true)\n" +
                "List<"+ fileName + "Dto> get"+ fileName + "Sn();");
    }

    public static void generateInterface(List<String> fields) throws IOException {
        String fileName = generateUpper(fields.get(0)) + (fields.size() > 1 ? generateUpper(fields.get(1))  : "");

        File entityFile = new File(filePath + "/" + fileName + "Dto.java" );
        entityFile.createNewFile();
        PrintWriter pw = new PrintWriter(entityFile);

        pw.println( "public interface " + fileName + "Dto {\n" );
        for(String s : fields){
            pw.println("    String get" + generateUpper(s) + "();");
            pw.println("");
        }
        pw.println("}");
        pw.close();
    }
}
