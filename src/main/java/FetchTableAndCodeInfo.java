import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.util.ListUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FetchTableAndCodeInfo {

    /**
     * 单次缓存的数据量
     */
    public static final int BATCH_COUNT = 100000;
    private static List<TableData> cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);

    private static String tableName = "";
    private static String tableenName = "";
    private static String tableEnName = "";
    private static String tableColName = "";
    private static String queryString = "";
    private static String sepString = ",t.";
    private static String packageName = "";

    private static String filePath = "D:/docs/codes/poc/files";
    public  static void main(String[] args) throws Exception {

        handInfo();
        generateData();
        generateFiles();
        generateEntity();
        generateController();
        generateInterface();
        generateService();
        generateDao();
        generateJpaDao();
        generateRepository();

    }

    public static void handInfo(){
        String fileName = "D:/docs/codes/poc/atds_device_fault.xlsx";

        tableName = new File(fileName).getName().split("\\.")[0];

        String[] parts = tableName.split("_");
        for(String part : parts){
            tableEnName += part.substring(0,1).toUpperCase() + part.substring(1);
        }
        tableenName = tableEnName.substring(0,1).toLowerCase() + tableEnName.substring(1);
        tableColName = tableName.toUpperCase() + "_COLUMN_FAMILY_NAME";

        System.out.println(tableName  + "---" + tableEnName);


        EasyExcel.read(fileName, TableData.class, new ReadListener<TableData>() {



             @Override
            public void invoke(TableData data, AnalysisContext context) {
                cachedDataList.add(data);
                System.out.println(data.getCol()+"-" + data.getType()+  ("[NULL]".equals(data.getDes())?"":("-"+ data.getDes() ) ));
                if(data.getType().contains("varchar") || data.getType().contains("text")){
                    data.setType("String");
                }else if (data.getType().contains("int")){
                    data.setType("Integer");
                }else if (data.getType().contains("bool")){
                    data.setType("boolean");
                }else if (data.getType().contains("timestamp")){
                    data.setType("Date");
                }
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                saveData();
            }

            /**
             * 加上存储数据库
             */
            private void saveData() {
            }
        }).sheet().doRead();

    }

    public static void generateFiles() throws IOException {
        //fields
        File fieldFile = new File(filePath + "/fields.java" );
        fieldFile.createNewFile();
        PrintWriter pw = new PrintWriter(fieldFile);
        pw.println( "public static final String " + tableColName + " = \"" + tableName + "\";" );
        for(TableData data : cachedDataList){

            queryString += sepString + fetchFieldEn(data.getCol());

            if("id".equalsIgnoreCase(data.getCol())
                || "create_time".equalsIgnoreCase(data.getCol())
                || "update_time".equalsIgnoreCase(data.getCol())
            ){
                continue;
            }

            pw.println( "public static final String " + (tableName + "_" + data.getCol()).toUpperCase() + " = \"" + data.getCol() + "\";" );
        }
        pw.close();

    }

    public static String fetchFieldEn(String s ){
        return fetchFieldEn(s,true);
    }
    public static String fetchFieldEn(String s , boolean start ){
        String[] parts = s.split("_");
        String result = "";
        boolean first = start;
        for(String part : parts){
            result += (first?part.substring(0,1):part.substring(0,1).toUpperCase()) + part.substring(1);
            first = false;
        }
        return result;
    }

    public static void generateEntity() throws IOException {
        //Entity
        File entityFile = new File(filePath + "/" + tableEnName + "Entity.java" );
        entityFile.createNewFile();
        PrintWriter pw = new PrintWriter(entityFile);

        pw.println("import com.itage.atds.dao.model.ModelConstants;\n" +
                "import com.itage.atds.repository.util.mapping.JsonStringType;\n" +
                "import lombok.AllArgsConstructor;\n" +
                "import lombok.Data;\n" +
                "import lombok.EqualsAndHashCode;\n" +
                "import lombok.NoArgsConstructor;\n" +
                "import org.hibernate.annotations.TypeDef;\n" +
                "import org.thingsboard.server.dao.model.BaseEntity;\n" +
                "import org.thingsboard.server.dao.model.BaseSqlEntity;\n" +
                "\n" +
                "import javax.persistence.Column;\n" +
                "import javax.persistence.Entity;\n" +
                "import javax.persistence.Table;\n" +
                "import java.util.Date;\n");

        pw.println( "@Data" );
        pw.println( "@AllArgsConstructor" );
        pw.println( "@NoArgsConstructor" );
        pw.println( "@EqualsAndHashCode(callSuper = true)");
        pw.println( "@Entity");
        pw.println( "@TypeDef(name = \"json\", typeClass = JsonStringType.class)");
        pw.println( "@Table(name = ModelConstants."  + tableColName + ")");
        pw.println( "public final class "+ tableEnName + "Entity extends BaseSqlEntity<"+ tableEnName + "> implements BaseEntity<"+ tableEnName + "> {"  );

        pw.println();

        for(TableData data : cachedDataList){

            if("id".equals(data.getCol())){
                continue;
            }

            if("create_time".equalsIgnoreCase(data.getCol())
            || "update_time".equalsIgnoreCase(data.getCol())){

                if("create_time".equalsIgnoreCase(data.getCol())){
                    pw.println( "   @Column(name = ModelConstants.CREATE_TIME_PROPERTY)");
                }else{
                    pw.println( "   @Column(name = ModelConstants.UPDATE_TIME_PROPERTY)");
                }
                pw.println( "   private " + data.getType() + " " + fetchFieldEn(data.getCol()) + ";" );
                pw.println();
                continue;
            }


            pw.println( "   @Column(name = ModelConstants." + tableName.toUpperCase() + "_" +  data.getCol().toUpperCase()  + ")"  );
            pw.println( "   private " + data.getType() + " " + fetchFieldEn(data.getCol()) + ";" );
            pw.println();
        }

        String d = tableEnName.substring(0,1).toLowerCase() + tableEnName.substring(1);

        //*********************第一个方法 domain->Entity的构造 ********************
        pw.println("    public " + tableEnName + "Entity(" + tableEnName + " " + d + "){");

        pw.println();

        for(TableData data : cachedDataList){
            if("id".equals(data.getCol())){
                pw.println("        this.id = toString(" + d + ".get" + data.getCol().substring(0,1).toUpperCase() + fetchFieldEn(data.getCol().substring(1)) + "());");
                continue;
            }

            pw.println("        this."+ fetchFieldEn(data.getCol()) + " = " + d + ".get" + data.getCol().substring(0,1).toUpperCase() + fetchFieldEn(data.getCol().substring(1)) + "();");
        }
        pw.println();
        pw.println("    }");

        pw.println();
        pw.println();

        //*********************第二个方法toData()********************
        pw.println("    @Override");
        pw.println("    public " + tableEnName + " toData(){");

        pw.println();

        pw.println( "       " +   tableEnName + " " + d + " = new " + tableEnName + "();");
        for(TableData data : cachedDataList){
            if("id".equals(data.getCol())){
                pw.println("       " + d + ".set" + fetchFieldEn(data.getCol(),false) + "(toUUID(this.id));");
                continue;
            }
            pw.println( "       " +  d + ".set" + fetchFieldEn(data.getCol(),false) + "(this." + fetchFieldEn(data.getCol()) +  ");");
        }
        pw.println();
        pw.println("       return " + d + ";");
        pw.println("    }");




        //构造方法
        pw.println();
        pw.println();

        //*********************第三个方法 参数列表Entity的构造 ********************
        pw.print("    public " + tableEnName + "Entity(");
        for(int i = 0 ; i < cachedDataList.size() ; i++ ){
            if(i>0){
                 pw.print(", ");
            }
            pw.print(cachedDataList.get(i).getType() + " " + cachedDataList.get(i).getCol().substring(0,1) + fetchFieldEn(cachedDataList.get(i).getCol().substring(1)));
        }
        pw.println("){");

        pw.println();

        for(TableData data : cachedDataList){
            pw.println("        this."+ fetchFieldEn(data.getCol()) + " = " + data.getCol().substring(0,1) + fetchFieldEn(data.getCol().substring(1)) + ";");
        }
        pw.println();
        pw.println("    }");

        pw.println("}");
        pw.close();
    }

    public static void generateData() throws IOException {
        File entityFile = new File(filePath + "/" + tableEnName + ".java" );
        entityFile.createNewFile();
        PrintWriter pw = new PrintWriter(entityFile);

        pw.println("import lombok.AllArgsConstructor;\n" +
                "import lombok.Data;\n" +
                "import lombok.NoArgsConstructor;\n" +
                "\n" +
                "import java.util.Date;\n" +
                "import java.util.UUID;\n");

        pw.println( "@Data\n" +
                "@AllArgsConstructor\n" +
                "@NoArgsConstructor\n" +
                "public class " + tableEnName  + " {\n");

        for(TableData data : cachedDataList){

            if( "id".equals(data.getCol())){
                pw.println("        private UUID " + fetchFieldEn(data.getCol()) + ";" );
            }else{
                pw.println("        private " + data.getType() + " " + fetchFieldEn(data.getCol()) + ";" );
            }

            pw.println();
        }

        pw.println("}");
        pw.close();
    }



    public static void generateController() throws IOException {
        File entityFile = new File(filePath + "/" + tableEnName + "Controller.java" );
        entityFile.createNewFile();
        PrintWriter pw = new PrintWriter(entityFile);

        pw.println("import com.itage.atds.common.data.BaseCriteria;\n" +
                "import com.itage.atds.controller.BaseController;\n" +
                "import org.springframework.beans.factory.annotation.Autowired;\n" +
                "import org.springframework.data.domain.Page;\n" +
                "import org.springframework.web.bind.annotation.*;\n" +
                "import java.util.List;\n");

        pw.println( "@RestController\n" +
                "@RequestMapping(BaseController.ROOTNAME + \"/" +
                 tableenName +"\")\n" +
                "public class " + tableEnName + "Controller {\n" );

        pw.println("    @Autowired\n" +
                "    private " + tableEnName + "Service " + tableenName + "Service;\n\n");

        pw.println("    @GetMapping(value = \"/findAll" + tableEnName + "s\")");
        pw.println("    public List<" + tableEnName + "> findAll" + tableEnName + "s(){\n" +
                "        return " + tableenName + "Service.findAll" + tableEnName + "s();\n" +
                "    }\n");

        pw.println("    @GetMapping(value = \"/find" + tableEnName + "sByPage\")\n" +
                "    public Page<" + tableEnName + "> find" + tableEnName + "sByPage(\n" +
                "            @RequestParam(required = true) int pageIndex,\n" +
                "            @RequestParam(required = true) int pageSize,\n" +
                "            @RequestParam(required = false) String sort,\n" +
                "            @RequestParam(required = false) String sortName){\n" +
                "\n" +
                "        BaseCriteria criteria = new BaseCriteria();\n" +
                "        criteria.setPageIndex(pageIndex);\n" +
                "        criteria.setPageSize(pageSize);\n" +
                "        criteria.setSort(sort);\n" +
                "        criteria.setSortName(sortName);\n" +
                "\n" +
                "        return " + tableenName + "Service.find" + tableEnName + "sByPage(criteria);\n" +
                "    }\n\n");

        pw.println("    @PostMapping(value = \"/save\")\n" +
                "    public String save(@RequestBody " + tableEnName + " " + tableenName + "){\n" +
                "        return " + tableenName + "Service.save(" + tableenName + ");\n" +
                "    }\n\n");

        pw.println(         "}\n" );
        pw.close();

    }

    public static void generateInterface() throws IOException {
        File entityFile = new File(filePath + "/" + tableEnName + "Service.java" );
        entityFile.createNewFile();
        PrintWriter pw = new PrintWriter(entityFile);

        pw.println("import com.itage.atds.common.data.BaseCriteria;\n" +
                "import org.springframework.data.domain.Page;\n" +
                "\n" +
                "import java.util.List;\n");

        pw.println( "public interface " + tableEnName + "Service {\n" );

        pw.println("    List<" + tableEnName + "> findAll" + tableEnName + "s();\n" +
                "\n" +
                "   Page<" + tableEnName + "> find" + tableEnName + "sByPage(BaseCriteria criteria);\n" +
                "\n" +
                "   String save(" + tableEnName + " " + tableenName + ");\n");

        pw.println("}");
        pw.close();
    }

    public static void generateService() throws IOException {
        File entityFile = new File(filePath + "/" + tableEnName + "ServiceImpl.java" );
        entityFile.createNewFile();
        PrintWriter pw = new PrintWriter(entityFile);

        pw.println("import com.itage.atds.common.data.BaseCriteria;\n" +
                "import com.itage.atds.common.data.UUIDConverter;\n" +
                "import lombok.extern.slf4j.Slf4j;\n" +
                "import org.springframework.beans.factory.annotation.Autowired;\n" +
                "import org.springframework.data.domain.Page;\n" +
                "import org.springframework.stereotype.Service;\n" +
                "import org.springframework.util.ObjectUtils;\n" +
                "\n" +
                "import java.util.Date;\n" +
                "import java.util.List;\n");

        pw.println("import com.itage.atds.common.data.BaseCriteria;\n" +
                "import org.springframework.data.domain.Page;\n" +
                "\n" +
                "import java.util.List;\n");

        pw.println( "@Slf4j\n" +
                "@Service\n" +
                "public class " + tableEnName + "ServiceImpl implements " + tableEnName + "Service {\n"
        );


        pw.println("    @Autowired\n" +
                "    private " + tableEnName + "Dao " + tableenName + "Dao;\n");

        pw.println("    @Override\n" +
                "    public List<" + tableEnName + "> findAll" + tableEnName + "s() {\n" +
                "        return " + tableenName + "Dao.find();\n" +
                "    }");


        pw.println("    @Override\n" +
                "    public Page<" + tableEnName + "> find" + tableEnName + "sByPage(BaseCriteria criteria) {\n" +
                "        return " + tableenName + "Dao.find" + tableEnName + "sByPage(criteria);\n" +
                "    }");

        pw.println("    @Override\n" +
                "    public String save(" + tableEnName + " " + tableenName + ") {\n" +
                dynamicSetTimeFiledsCode(tableenName) +
                "        " + tableEnName + " save = " + tableenName + "Dao.save(" + tableenName + ");\n\n" +
                "        return UUIDConverter.fromTimeUUID(save.getId());\n" +
                "    }");

        pw.println( "}");
        pw.close();
    }


    /**
     * 오품호
     * @return
     */
    private static String dynamicSetTimeFiledsCode(String tableename){

        String strResult = "\n";

        List<String> cachedColNameList = cachedDataList.stream().map(e -> e.getCol()).collect(Collectors.toList());

        if(cachedColNameList.contains("create_time")){
            strResult += "        Date current = new Date();\n" +
                    "\n" +
                    "        if(ObjectUtils.isEmpty(" + tableename +
                    ".getId())){\n" +
                    "            " +
                    tableename + ".setCreateTime(current);\n" +
                    "        }\n";
        }

        strResult += "\n";

        if(cachedColNameList.contains("create_time") && cachedColNameList.contains("update_time")){ //有修改时间必有创建时间
            strResult += "        " + tableenName + ".setUpdateTime(current);\n";
        }

        return strResult;
    }

    public static void generateDao() throws IOException {
        File entityFile = new File(filePath + "/" + tableEnName + "Dao.java" );
        entityFile.createNewFile();
        PrintWriter pw = new PrintWriter(entityFile);

        pw.println("import com.itage.atds.common.data.BaseCriteria;\n" +
                "import org.springframework.data.domain.Page;\n" +
                "import org.thingsboard.server.dao.Dao;\n");

        pw.println( "public interface " + tableEnName + "Dao extends Dao<" + tableEnName + "> {\n");

        pw.println( "   Page<" + tableEnName + "> find" + tableEnName + "sByPage(BaseCriteria criteria);\n");

        pw.println( "}");
        pw.close();
    }

    public static void generateJpaDao() throws IOException {
        File entityFile = new File(filePath + "/Jpa" + tableEnName + "Dao.java" );
        entityFile.createNewFile();
        PrintWriter pw = new PrintWriter(entityFile);

        pw.println("import com.itage.atds.common.data.BaseCriteria;\n" +
                "import com.itage.atds.repository.util.SqlDao;\n" +
                "import org.apache.commons.lang3.StringUtils;\n" +
                "import org.springframework.beans.factory.annotation.Autowired;\n" +
                "import org.springframework.data.domain.*;\n" +
                "import org.springframework.data.repository.CrudRepository;\n" +
                "import org.springframework.stereotype.Component;\n" +
                "import org.thingsboard.server.dao.DaoUtil;\n" +
                "import org.thingsboard.server.dao.sql.JpaAbstractDao;\n");

        pw.println( "@Component\n" +
                "@SqlDao\n" +
                "public class Jpa" + tableEnName + "Dao extends JpaAbstractDao<" + tableEnName + "Entity, " + tableEnName + "> implements " + tableEnName + "Dao {\n" );

        pw.println("    @Autowired\n" +
                "    private " + tableEnName + "Repository " + tableenName + "Repository;");

        pw.println("    @Override\n" +
                "    protected Class<" + tableEnName + "Entity> getEntityClass() {\n" +
                "        return " + tableEnName + "Entity.class;\n" +
                "}\n" );

        pw.println("    @Override\n" +
                "    protected CrudRepository<" + tableEnName + "Entity, String> getCrudRepository() {\n" +
                "        return " + tableenName + "Repository;\n" +
                "    }\n" );

        pw.println("    @Override\n" +
                "    public Page<" + tableEnName + "> find" + tableEnName + "sByPage(BaseCriteria criteria) {\n" +
                "\n" +
                "        String sortName = StringUtils.isBlank(criteria.getSortName()) ? \"createTime\" : criteria.getSortName();\n" +
                "        Sort.Direction sortType = StringUtils.isBlank(criteria.getSort())\n" +
                "                ? Sort.Direction.DESC : Sort.Direction.fromString(criteria.getSort());\n" +
                "        Sort sort = new Sort(sortType, sortName);\n" +
                "\n" +
                "        Pageable pageable = PageRequest.of(criteria.getPageIndex(), criteria.getPageSize(), sort);\n" +
                "\n" +
                "\n" +
                "        Page<" + tableEnName + "Entity> page = " + tableenName + "Repository.page(pageable);\n" +
                "\n" +
                "        return new PageImpl<>(DaoUtil.convertDataList(page.getContent()), page.getPageable(), page.getTotalElements());\n" +
                "    }");

        pw.println("}");
        pw.close();
    }

    public static void generateRepository() throws IOException {
        File entityFile = new File(filePath + "/" + tableEnName + "Repository.java" );
        entityFile.createNewFile();
        PrintWriter pw = new PrintWriter(entityFile);

        pw.println("import com.itage.atds.repository.util.SqlDao;\n" +
                "import org.springframework.data.domain.Page;\n" +
                "import org.springframework.data.domain.Pageable;\n" +
                "import org.springframework.data.jpa.repository.JpaSpecificationExecutor;\n" +
                "import org.springframework.data.jpa.repository.Query;\n" +
                "import org.springframework.data.repository.CrudRepository;\n");

        pw.println( "@SqlDao\n" +
                "public interface " + tableEnName + "Repository extends CrudRepository<" + tableEnName + "Entity, String>, JpaSpecificationExecutor<" + tableEnName + "Entity> {\n" );

        pw.println("    @Query(value = \"select new " + "全类名Entity替换(\" +\n\t"  +
                        "\"" + queryString.substring(1) + "\" +\n\t\t" +
                "\"" + ") from " + tableEnName + "Entity t\")\n" +
                "   Page<" + tableEnName + "Entity> page(Pageable pageable);\n");

        pw.println("}");
        pw.close();
    }

}
