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

    // 대기 필요 시 응답 교체
    //if (WG_IsNeedToWaiting($WG_SERVICE_ID, $WG_GATE_ID))
    if (false == WG_IsValidToken($WG_SERVICE_ID, $WG_GATE_ID))
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
    <meta http-equiv='cache-control' content='no-cache' />
    <meta http-equiv='Expires' content='-1' />
    <title></title>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <!-- custom css -->
    <link href="https://dist.devy.kr/bulma/0.7.1/bulma.css" rel="stylesheet" />


</head>
<body>

    <div id="app" class="container">
        <p class="title has-text-info">SAMPLE BACKEND PAGE</p>
        <p class="subtitle has-text-danger">GATE ID : <?php print($WG_GATE_ID) ?> </p>
        <div class="notification is-dark">
            <h2 class="has-text-warning">Backend 방식의 유량제어가 적용된 SAMPLE 업무페이지입니다.</h2>
            <h2 class="has-text-warning">GATE가 TEST-MODE 체크되었다면 첫 진입 시 대기표가 발급되어 대기UI가 5~10초 표시됩니다.</h2>
			<h2 class="has-text-warning">대기완료 후 재진입(F5)은 AdminPage의 FreePass 설정만큼 Pass됩니다.</h2>
			<h2 class="has-text-warning">AdminPage에서 FreePass 즉시 초기화 버튼을 클릭하면 대기표가 다시 발급되고, TEST MODE 또는 초당 유입량이 설정값을 초과하는 경우 대기가 진행됩니다.</h2>
        </div>

        <hr/>
        <a class="button is-danger" href="backend_with_intro.php">INTRO 방식 페이지 가기</a>


        <hr/>
        <pre>
        <p>JAVA 호출코드 예시</p>
        <textarea class="textarea" rows="20" readonly>
        ...
    	String serviceId 	= "<?php print($WG_SERVICE_ID)?>"; 	// 할당된 SERVICE ID 
    	String gateId 		= "<?php print($WG_GATE_ID)?>";  	    // 사용할 GATE ID (할당된 GATE ID 범위내에서 사용)
    	
    	WebGate webgate = new WebGate();
    	//대기표 검증하여 유효하지 않으면 대기UI 화면 컨텐츠로 응답 교체
    	if(webgate.WG_IsNeedToWaiting(serviceId, gateId, request, response))
    	{
    		try {
    			String uiHtml = webgate.WG_GetWaitingUi(serviceId, gateId);
    			response.setContentType("text/html");
        		PrintWriter out = response.getWriter();
    			out.write(uiHtml);
    			out.close();
    			return ...; // 환경(Framework)에 따라 void OR mapping url string을 return....
	    	} catch (Exception e) {
	    		// 필요시 log write..
	    	}
	        finally {}
    	}         ...
        // 여기에서 부터 기존 업무로직 시작...
        </textarea>
        </pre>


        <pre>
        <p>PHP 호출코드 예시</p>
        <textarea class="textarea" rows="20" readonly>
        ...
        require_once("webgate-lib.php");
        // setting 
        $WG_SERVICE_ID  = "<?php print($WG_SERVICE_ID)?>";    // 고정값(fixed)
        $WG_GATE_ID     = "<?php print($WG_GATE_ID)?>"; // 할당받은 GATE ID 중에서 사용

        // 대기 필요 시 응답 교체
        if (WG_IsNeedToWaiting($WG_SERVICE_ID, $WG_GATE_ID))
        {
            print WG_GetWaitingUi($WG_SERVICE_ID, $WG_GATE_ID);
            exit(); // 응답종료 
        }
        ... 
		// 여기에서 부터 기존 업무로직 시작...
        </textarea>
        </pre>
    </div>


</body>
</html>
