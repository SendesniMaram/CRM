package com.crm.hr;
import com.crm.hr.repository.HrStatisticsRepository;import org.junit.jupiter.api.*;import org.springframework.data.jpa.repository.Query;import org.springframework.jdbc.datasource.SingleConnectionDataSource;import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;import java.sql.*;import java.time.*;import java.util.*;import static org.junit.jupiter.api.Assertions.*;
/** Executes aggregate logic on small synthetic H2 tables, without Spring Boot/Flyway.
 * MySQL DATE_ADD uses the H2 DATEADD spelling. The recursive CTE uses literal fixture dates
 * because bound recursive CTE dates return no rows on H2 2.2.224. JDBC/MySQL binding is not proved. */
class HrAggregateFixtureTest {
 Connection connection;JdbcTemplate jdbc;NamedParameterJdbcTemplate named;
 @BeforeEach void setup()throws Exception{connection=DriverManager.getConnection("jdbc:h2:mem:hr3c_"+UUID.randomUUID()+";MODE=MySQL");var source=new SingleConnectionDataSource(connection,true);jdbc=new JdbcTemplate(source);named=new NamedParameterJdbcTemplate(source);
  jdbc.execute("create table leave_requests(employee_id varchar(50),start_date date,end_date date,status varchar(20))");
  jdbc.execute("create table trainings(start_date date,end_date date,status varchar(20))");jdbc.execute("create table employee_evaluations(period_start date,period_end date,status varchar(20))");
  jdbc.execute("create table job_offers(created_at timestamp,status varchar(20))");jdbc.execute("create table job_applications(created_at timestamp,status varchar(20))");
  jdbc.update("insert into leave_requests values ('101','2025-12-31','2026-01-02','APPROVED'),('101','2026-01-02','2026-01-03','APPROVED'),('102','2026-01-03','2026-01-03','APPROVED'),('101','2026-01-01','2026-01-02','PENDING'),('102','2026-01-01','2026-01-31','REJECTED')");
  jdbc.update("insert into trainings values ('2026-01-01','2026-01-01','PLANNED'),('2025-12-30','2026-01-01','COMPLETED'),('2026-02-01','2026-02-03','CANCELLED')");
  jdbc.update("insert into employee_evaluations values ('2025-12-01','2026-01-02','FINALIZED'),('2026-02-01','2026-02-28','DRAFT')");
  jdbc.update("insert into job_offers values ('2026-01-01 00:00:00','OPEN'),('2025-12-31 23:59:59','CLOSED')");
  jdbc.update("insert into job_applications values ('2026-01-01 00:00:00','RECEIVED'),('2026-01-03 23:59:59','ACCEPTED'),('2026-01-04 00:00:00','REJECTED')");
 }
 @AfterEach void close()throws Exception{connection.close();}
 String query(String method,Class<?> type)throws Exception{return HrStatisticsRepository.class.getMethod(method,type,type).getAnnotation(Query.class).value().replace("DATE_ADD(d,INTERVAL 1 DAY)","DATEADD('DAY',1,d)");}
 Map<String,Object> dates(String from,String to){return Map.of("from",LocalDate.parse(from),"to",LocalDate.parse(to));}
 Map<String,Long> counts(String method)throws Exception{var result=new HashMap<String,Long>();named.query(query(method,LocalDate.class),dates("2026-01-01","2026-01-03"),(org.springframework.jdbc.core.RowCallbackHandler)row->result.put(row.getString("status"),row.getLong("total")));return result;}
 long approvedDays(String from,String to)throws Exception{
  String sql=query("approvedEmployeeDays",LocalDate.class).replace(":from","DATE '"+LocalDate.parse(from)+"'").replace(":to","DATE '"+LocalDate.parse(to)+"'");
  return jdbc.queryForObject(sql,Long.class);
 }
 @Test void overlappingRequestsAreCountedByStatusOnKnownData()throws Exception{assertEquals(Map.of("APPROVED",3L,"PENDING",1L,"REJECTED",1L),counts("leaves"));}
 @Test void approvedDaysAreUniquePerEmployeeAndClippedToPeriod()throws Exception{assertEquals(4L,approvedDays("2026-01-01","2026-01-03"));assertEquals(1L,approvedDays("2026-01-02","2026-01-02"));}
 @Test void trainingAndEvaluationPeriodsReallyOverlap()throws Exception{assertEquals(Map.of("PLANNED",1L,"COMPLETED",1L),counts("trainings"));assertEquals(Map.of("FINALIZED",1L),counts("evaluations"));}
 @Test void recruitmentCreationUsesInclusiveStartAndExclusiveNextDay()throws Exception{var params=Map.of("from",LocalDate.parse("2026-01-01").atStartOfDay(),"until",LocalDate.parse("2026-01-04").atStartOfDay());for(String method:List.of("offers","applications")){var rows=named.queryForList(query(method,LocalDateTime.class),params);long total=rows.stream().mapToLong(row->((Number)row.get("total")).longValue()).sum();assertEquals(method.equals("offers")?1:2,total);}}
 @Test void emptyPeriodProducesNoInventedRowsOrDays()throws Exception{assertTrue(named.queryForList(query("leaves",LocalDate.class),dates("2027-01-01","2027-01-03")).isEmpty());assertEquals(0L,approvedDays("2027-01-01","2027-01-03"));}
}