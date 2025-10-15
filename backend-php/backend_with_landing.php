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
    $WG_GATE_ID      = "4";    // 할당받은 GATE ID 중에서 사용
    $WG_SERVICE_ID   = "9000"; // 고정값(fixed)

    // 유량제어 체크 : 토큰 유효하지 않으면 재발급(=대기UI 응답)
    if (false == WG_IsValidToken($WG_SERVICE_ID, $WG_GATE_ID))
    {
        // 현재 페이지의 URL 구하기
        $currentUrl = $_SERVER['REQUEST_URI'];

        // intro.html로 리다이렉트 (URL 인코딩 포함)
        $nextUrl = urlencode($currentUrl);
        header("Location: landing.html?GateId={$WG_GATE_ID}&NextUrl={$nextUrl}");

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

    <meta http-equiv='cache-control' content='no-cache' />
    <meta http-equiv='Expires' content='-1' />

    <!-- custom css -->
    <link href="https://dist.devy.kr/bulma/0.7.1/bulma.css" rel="stylesheet" />

    <style>
        html, body {
            width:100%;
            height:100%;
        }


    </style>

</head>
<body>

    <div id="app" class="container">
        <form id="form1">
            <p class="title has-text-info">여기는 대문 페이지 입니다.</p>
            <p class="subtitle has-text-danger">GATE ID : <?php print($WG_GATE_ID) ?> </p>
            <div class="notification is-dark">
                <h1 class="subtitle has-text-white">● INTRO 방식 샘플 페이지입니다.</h1>
                <h1 class="subtitle has-text-white">● 최초 접속이나, FreePass 초과(새로고침 반복시도, 시간제한 등) 시 INTRO 페이지를 다시 다녀옵니다.</h1>
                <h1 class="subtitle has-text-white">● 유입차단 기간에는 INTRO 페이지로 강제 이동된 후 재 진입이 차단됩니다.</h1>
                <h1 class="subtitle has-text-warning">※ 데모를 위해 유입차단 기능이 동작 중입니다. (매 10분 단위로 2분간(**:*8:00 ~ **:*9:59)</h1>
                <h1 class="subtitle has-text-warning">※ 데모를 위해 FreePass는 5분, 10회로 설정되어 있습니다. (초과 시 INTRO 페이지 다녀옵니다)</h1>
            </div>
            <div class="">
                <a href="landing.html?IsDevTest=Y" class="button dark">LANDING 페이지 샘플</a>
            </div>
        </form>

        <hr/>
        <pre>
        <p>JAVA 호출코드 예시</p>
        <textarea class="textarea" rows="20" readonly>
        ...
    	String serviceId 	= "<?php print($WG_SERVICE_ID)?>"; 	// 할당된 SERVICE ID 
    	String gateId 		= "<?php print($WG_GATE_ID)?>";  	// 사용할 GATE ID (할당된 GATE ID 범위내에서 사용)
    	
    	WebGate webgate = new WebGate();
    	// 대기표 검증
    	if(false == webgate.WG_IsValidToken(serviceId, gateId, request, response))
    	{
            // 현재 요청 경로 + 쿼리
            StringBuilder currentPath = new StringBuilder(request.getRequestURI());
            if (request.getQueryString() != null) {
                currentPath.append("?").append(request.getQueryString());
            }

            // URL 인코딩 (UTF-8)
            String nextUrl = URLEncoder.encode(currentPath.toString(), "UTF-8");

            // 동일 서버 내 landing.html로 리다이렉트
    		return "redirect:/landing.html?GateId=" + gateId + "&NextUrl=" + nextUrl;  // OR response.sendRedirect("landing.html?GateId=" + gateId + "&NextUrl=" + nextUrl);
    	} 
        ...
        // 여기에서 부터 기존 업무로직 시작...
        </textarea>
        </pre>


        <pre>
        <p>PHP 호출코드 예시</p>
        <textarea class="textarea" rows="20" readonly>
        ...
        require_once("webgate-lib.php");
        // setting 
        $WG_SERVICE_ID   = "<?php print($WG_SERVICE_ID)?>"; // 고정값(fixed)
        $WG_GATE_ID      = "<?php print($WG_GATE_ID)?>";    // 할당받은 GATE ID 중에서 사용

        // 유량제어 체크 : 토큰 유효하지 않으면 재발급(=대기UI 응답)
        if (!WG_IsValidToken($WG_SERVICE_ID, $WG_GATE_ID))
        {
            // 현재 페이지의 URL 구하기
            $currentUrl = $_SERVER['REQUEST_URI'];

            // intro.html로 리다이렉트 (URL 인코딩 포함)
            $nextUrl = urlencode($currentUrl);
            header("Location: landing.html?GateId={$WG_GATE_ID}&NextUrl={$nextUrl}");

            exit(); // 응답종료 
        }
        ... 
		// 여기에서 부터 기존 업무로직 시작...
        </textarea>
        </pre>
    </div>
     

</body>
</html>
