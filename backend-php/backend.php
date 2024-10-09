<?php

    /* debug 필요 시
    error_reporting(E_ALL);
    ini_set("display_errors", 1);
    */

    /* 
    * ==============================================================================================
    * 메가펜스 유량제어서비스 SAMPLE(PHP)
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
    //print "REFERER:" . $_SERVER['HTTP_REFERER'];

    /* ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼ BEGIN OF 유량제어 코드삽입 */
    // import library
    require_once("webgate-lib.php");
    // setting 
    $WG_GATE_ID      = "1";    // 할당받은 GATE ID 중에서 사용
    $WG_SERVICE_ID   = "9000"; // 고정값(fixed)

    // 유량제어 체크 : 토큰 유효하지 않으면 재발급(=대기UI 응답)
    if (!WG_IsValidToken($WG_SERVICE_ID, $WG_GATE_ID))
    {
        print WG_GetWaitingUi($WG_SERVICE_ID, $WG_GATE_ID);
        exit(); // 응답종료 
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
    <link href="https://dist.devy.kr/bulma-0.7.1/bulma.css" rel="stylesheet" />

    <style>
        html, body {
            width:100%;
            height:100%;
            overflow-y : hidden;

        }


    </style>

</head>
<body>


    <div id="app" class="container">
        <form id="form1">
            <div class="notification is-info">
                <h1 class="title">[DEMO] WELLCOME, TO THE MOON TRAVEL!!!</h1>
                <h1 class="subtitle has-text-success">Backend 호출코드 적용된 페이지입니다. 새로고침을 여러번 시도하면 대기 화면을 다시 만나게 됩니다.</h1>
            </div>

            <div class="notification is-white has-text-centered" style="position:fixed; width:100%; left:0; bottom:0; background-color:#000000ee;">
                <h2 class="has-text-light">Backend 방식의 유량제어가 적용된 SAMPLE 업무페이지입니다.</h2>
                <h2 class="has-text-light">CDN Landing 방식을 거쳐서 왔다면 바로 열리지만 새로고침이나 직접 접속하는 경우 유량제어가 동작합니다.</h2>
                <h2 class="has-text-light">Backend 방식은 대문(index), 이벤트 안내, 상품상세, 주문하기 페이지 등 Backend 코드(JAVA/JSP/PHP/ASP.NET) 삽입이 가능한 모든 페이지에 적용할 수 있습니다.</h2>
            </div>
        </form>
    </div>

</body>
</html>
