<?php
    /* debug 필요 시
    error_reporting(E_ALL);
    ini_set("display_errors", 1);
    */

    /* 
    * ==============================================================================================
    * 메가펜스 유량제어서비스 SAMPLE(PHP) V.210403
    * 이 샘플소스는 메가펜스 서비스 계약 및 테스트(POC) 고객에게 제공됩니다.
    * 오류조치 및 개선을 목적으로 자유롭게 수정 가능하며 해당 내용은 공급처에 통보 바랍니다.
    * 허가된 고객 이외의 무단 복사, 배포, 수정, 동작 등 일체의 이용을 금합니다.
    * 작성자 : ysd@devy.co.kr
    * ----------------------------------------------------------------------------------------------
    * REPLACE 방식의 유량제어를 적용한 고객사 샘플 업무페이지 입니다.
    * <이용 안내> 
    *   ⊙ 아래의 샘플코드를 그대로 테스트용 페이지에 삽입해서 대기UI가 표시되는지 확인
    *   ⊙ 서비스 세팅이 완료되면 안내받은 GATE_ID, SERVICE_ID로 수정해서 사용
    *
    * <주의 사항>
    *   ⊙ 유량제어 코드는 DB접속 등의 부하량이 많은 업무로직 이전에 삽입해야 효과적입니다.
    *   ⊙ 쿠키나 세션 등을 이용하는 간단한 처리는 유량제어 코드 이전에 배치되어도 무방합니다.
    * ==============================================================================================
    */

    /* ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼ BEGIN OF 유량제어 코드삽입 */
    // import library
    require_once("webgate-lib.php");
    // setting 
    $WG_GATE_ID      = 1;    // 할당받은 GATE ID 중에서 사용
    $WG_SERVICE_ID   = 9000; // 고정값(fixed)

    // 유량제어 체크 : 접속자가 많으면 대기UI로 응답 대체
    if (WG_IsNeedToWaiting($WG_SERVICE_ID, $WG_GATE_ID))
    {
        print WG_GetWaitingUi($WG_SERVICE_ID, $WG_GATE_ID);
        return; // 응답종료
    }
    /* ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲ END OF 유량제어 코드삽입 */


    // 축하합니다!!!
    // 여기까지 진입했다면 대기열 체크가 완료되었으므로 원래의 업무페이지 컨텐츠를 표시합니다.
?>

<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <title></title>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <!-- custom css -->
    <link href="//cdn.devy.kr/dist/bulma-0.7.1/bulma.css" rel="stylesheet" />
</head>
<body>
    <div id="app" class="container">
        <form id="form1">
            <div class="notification is-info">
                <h1 class="title">Hello, World!!!</h1>
                <h2 class="has-text-light">Backend 방식의 유량제어를 적용한 SAMPLE 업무페이지.</h2>
                <h2 class="has-text-light">대문(index), 이벤트 안내, 상품상세, 주문하기 페이지 등 Backend 코드(JAVA/JSP/PHP/ASP.NET) 삽입이 가능한 모든 페이지에 적용할 수 있습니다.</h2>
            </div>

            <hr/>
            <div class="notification is-white has-text-centered">
                <span class="tag is-light is-large is-rounded">다른 데모 : </span>
                <a class="button is-dark" href="frontend.html">Frontend 방식 DEMO</a> <a class="button is-dark" href="landing.html">Landing 방식 DEMO</a>
            </div>
        </form>
    </div>
</body>
</html>
