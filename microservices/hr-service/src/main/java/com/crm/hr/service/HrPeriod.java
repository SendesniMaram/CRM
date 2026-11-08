package com.crm.hr.service;
import java.time.LocalDate;import java.time.temporal.ChronoUnit;
/** Inclusive civil dates, limited to 366 days. No legal acquisition or working-day rule. */
public final class HrPeriod {private HrPeriod(){} public static void require(LocalDate from,LocalDate to){if(from==null||to==null||to.isBefore(from)||from.getYear()<1||to.getYear()>9998||ChronoUnit.DAYS.between(from,to)>365)throw new IllegalArgumentException("Choose an inclusive period of at most 366 calendar days");}}