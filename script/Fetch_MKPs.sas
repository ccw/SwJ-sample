LIBNAME RM 'D:\SAS\RM';

PROC SQL;
   CREATE TABLE QUERY_FOR_MARKETING_PLAN AS
   SELECT /* PERIOD_START_DT */
          INTNX('WEEK', "&QRY_START_DT"D, 0) AS PERIOD_START_DT,
          /* PERIOD_END_DT */
          INTNX('DAY', INTNX('WEEK', "&QRY_END_DT"D, 0), 6) AS PERIOD_END_DT,
          t1.CAMPAIGN_CD,
          t1.CAMPAIGN_NM,
          t1.CAMPAIGN_DESC,
          t1.CHANNEL_CD,
          t1.DEPARTMENT_CD,
          t1.CAMPAIGN_START_DT,
          t1.CAMPAIGN_END_DT,
          /* S */
          (CASE WHEN t1.CAMPAIGN_START_DT - INTNX('WEEK', "&QRY_START_DT"D, 0) > 0
                THEN t1.CAMPAIGN_START_DT - INTNX('WEEK', "&QRY_START_DT"D, 0) + 1
                ELSE 1 END) AS S,
          /* E */
          (CASE WHEN t1.CAMPAIGN_END_DT > INTNX('DAY', INTNX('WEEK', "&QRY_END_DT"D, 0), 6)
                THEN INTNX('DAY', INTNX('WEEK', "&QRY_END_DT"D, 0), 6) - INTNX('WEEK', "&QRY_START_DT"D, 0) + 1
                ELSE t1.CAMPAIGN_END_DT - INTNX('WEEK', "&QRY_START_DT"D, 0) END) + 1 AS E,
          /* YEAR */
          YEAR((INTNX('WEEK', "&QRY_START_DT"D, 0))) AS YEAR,
          /* MONTH */
          MONTH((INTNX('WEEK', "&QRY_START_DT"D, 0))) AS MONTH,
          /* DAY */
          DAY((INTNX('WEEK', "&QRY_START_DT"D, 0))) AS DAY,
          /* PERIOD */
          (INTNX('DAY', INTNX('WEEK', "&QRY_END_DT"D, 0), 6)) - (INTNX('WEEK', "&QRY_START_DT"D, 0)) AS PERIOD
      FROM RM.MARKETING_PLAN t1
      WHERE t1.CAMPAIGN_START_DT BETWEEN "&QRY_START_DT"d AND "&QRY_END_DT"d OR
            t1.CAMPAIGN_END_DT BETWEEN "&QRY_START_DT"d AND "&QRY_END_DT"d
      ORDER BY t1.CHANNEL_CD, t1.DEPARTMENT_CD, t1.CAMPAIGN_START_DT;
QUIT;

DATA _NULL_;
    file print PS=32767 NOTITLES;
    set QUERY_FOR_MARKETING_PLAN end=lastrec;

    retain CURRENT_CHANNEL_CD CURRENT_DEPARTMENT_CD;

    if _N_ eq 1 then do;
        WEEK = INTINDEX('WEEK', PERIOD_START_DT);
        WEEKS = INTCK('WEEK', PERIOD_START_DT, PERIOD_END_DT);

        put '{"year":' YEAR ',';
        put '"month":' MONTH ',';
        put '"day":' DAY ',';
        put '"week":' WEEK ',';
        put '"weeks":' WEEKS ',';
        put '"period":' PERIOD ',';
        put '"campagins":[{';
        put '"channel":"' CHANNEL_CD '",';
        put '"departs":[{';
        put '"name":"' DEPARTMENT_CD '",';
        put '"events":[';

        CURRENT_CHANNEL_CD = CHANNEL_CD;
        CURRENT_DEPARTMENT_CD = DEPARTMENT_CD;

    end;
    else if CURRENT_CHANNEL_CD ^= CHANNEL_CD then do;
        put ']},{';
        put '"channel":"' CHANNEL_CD '",';
        put '"departs":[{';
        put '"name":"' DEPARTMENT_CD '",';
        put '"events":[';

        CURRENT_CHANNEL_CD = CHANNEL_CD;
        CURRENT_DEPARTMENT_CD = DEPARTMENT_CD;

    end;
    else if CURRENT_DEPARTMENT_CD ^= DEPARTMENT_CD then do;
        put ']},{';
        put '"name":"' DEPARTMENT_CD '",';
        put '"events":[';

        CURRENT_DEPARTMENT_CD = DEPARTMENT_CD;
    end;
    else do;
        put ',';
    end;

    BDATE = PUT(CAMPAIGN_START_DT, YYMMDDS10.);
    EDATE = PUT(CAMPAIGN_START_DT, YYMMDDS10.);

    put '{"name":"' CAMPAIGN_NM '",';
    put '"begin_date":"' BDATE '",';
    put '"end_date":"' EDATE '",';
    put '"s":' S ',';
    put '"e":' E ',';
    put '"memo":"' CAMPAIGN_DESC '"}';

    if lastrec eq 1 then do;
        put ']}]}]}]}';
    end;

RUN;