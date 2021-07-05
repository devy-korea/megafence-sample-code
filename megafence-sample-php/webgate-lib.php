<?php
    /* 
    * ==============================================================================================
    * 메가펜스 유량제어서비스 공통모듈(PHP) V.21.1.1
    * 이 라이브러리는 메가펜스 서비스 계약 및 테스트(POC) 고객에게 제공됩니다.
    * 오류조치 및 개선을 목적으로 자유롭게 수정 가능하되 해당 내용은 반드시 공급처에 통보해야 합니다.
    * 허가된 고객 및 환경 이외에서의 무단 복사, 배포, 수정, 동작 등 일체의 이용을 금합니다.
    * 작성자 : ysd@devy.co.kr
    * ----------------------------------------------------------------------------------------------
    * 2021-01-20 : 부하발생용 parameter 처리
    * 	            api call timeout 1초 --> 2초
    * 2021-03-24 : response.setContentType() 처리 추가
    * 2021-04-03 : UI응답부 template fileload 대체
    *              server list update
    * V.21.1.1 (2021-06-28) ------------------------------------------------------------------------
    *   [minor fix] WG_GetWaitingUi() : html & body style (width 100 --> 100%)
    *   [minor fix] WG_GetWaitingUi() : remove whitespace starting html template($html)
    *   [fix] WG_GetRandomString() index overflow
    * ==============================================================================================
    */


    
    function WG_IsNeedToWaiting($service_id, $gate_id)
    {

        $WG_VERSION         = "V.21.1.2";
        $WG_SERVICE_ID      = $service_id;            
        $WG_GATE_ID         = $gate_id;              
        $WG_MAX_TRY_COUNT   = 3;            // [fixed] failover api retry count
        $WG_IS_CHECKOUT_OK  = false;        // [fixed] 대기를 완료한 정상 대기표 여부 (true : 대기완료한 정상 대기표, false : 정상대기표 아님)
        $WG_GATE_SERVERS    = array (       // [fixed] 대기표 발급서버 Address List
		    "9000-0.devy.kr",
            "9000-1.devy.kr",
            "9000-2.devy.kr",
            "9000-3.devy.kr",
            "9000-4.devy.kr",
		    "9000-5.devy.kr",
            "9000-6.devy.kr",
            "9000-7.devy.kr",
            "9000-8.devy.kr",
            "9000-9.devy.kr"); 

        $WG_TOKEN_NO        = "";           // 대기표 ID
        $WG_TOKEN_KEY       = "";           // 대기표 key
        $WG_WAS_IP          = "";           // 대기표 발급서버
        $WG_TRACE           = "";           // TRACE 정보 (쿠키응답)
        $WG_IS_LOADTEST     = "N";          // jmeter 등으로 발생시킨 요청인지 여부



	    /*
        JMeter 등에서 부하테스트(LoadTest)용으로 호출된 경우를 위한 처리 (부하발생 시 URL에 IsLoadTest=Y parameter 추가해야 합니다)
	    */
        if($WG_IS_LOADTEST != null || $WG_IS_LOADTEST == "Y" )
        {
            $WG_IS_LOADTEST = "Y";
        }

        // Timeout 제어 (2초이내 무응답 장애간주)
        $WG_SOCKET_TIMEOUT = ini_get("default_socket_timeout");
        ini_set("default_socket_timeout", 2);


        /******************************************************************************
        STEP-1 : URL Prameter로 대기표 검증 (CDN Landing 방식을 이용하는 경우에 해당)
        *******************************************************************************/
        try 
        {
            if(isset($_GET["WG_TOKEN"])) 
            {
                $WG_TRACE .= "STEP1, ";

                // WG_TOKEN paramter를 '|'로 분리 및 분리된 개수 체크
                $parameterValues = explode(",", $_GET["WG_TOKEN"]);
                if (count($parameterValues) == count(explode(",", "GATE_ID,TOKEN_NO,TOKEN_KEY,WAS_IP")))
                {
                    // WG_TOKEN parameter에 세팅된 값 GET
                    $WG_TOKEN_NO    = $parameterValues[1];
                    $WG_TOKEN_KEY   = $parameterValues[2];
                    $WG_WAS_IP      = $parameterValues[3];

                    if( $WG_TOKEN_NO     !== null   && $WG_TOKEN_NO  !=="" 
                        && $WG_TOKEN_KEY !== null   && $WG_TOKEN_KEY !== "" 
                        && $WG_WAS_IP    !== null   && $WG_WAS_IP    !== "")
                    {
                        // 대기표 Validation(checkout api call)
                        $apiUrl = "http://" . $WG_WAS_IP . "/?ServiceId=" . $WG_SERVICE_ID . "&GateId=" . $WG_GATE_ID . "&Action=OUT&TokenNo=" . $WG_TOKEN_NO . "&TokenKey=" . $WG_TOKEN_KEY . "&IsLoadTest=" . $WG_IS_LOADTEST;
                        $responseText = file_get_contents($apiUrl);
                        if($responseText != null && $responseText != "" && strpos($responseText, "\"ResultCode\":0") !== false)
                        {
                            $WG_IS_CHECKOUT_OK = true;
                        }
                    }
                }
            } 
        }
        catch(Exception $e) 
        {
            $WG_TRACE .= "ERROR:".$e->getMessage().", ";
            // ignore & goto next
        }

        /******************************************************************************
        STEP-2 : Cookie로 대기표 검증 (CDN Landing 방식 이외의 일반적인 방식에 해당)
        *******************************************************************************/
        try 
        {
            if($WG_IS_CHECKOUT_OK == false)
            {
                $WG_TRACE .= "STEP2, ";
                if(isset($_COOKIE["WG_TOKEN_NO"]))  { 
                    $WG_TOKEN_NO  = $_COOKIE["WG_TOKEN_NO"]; 
                }
                if(isset($_COOKIE["WG_CLIENT_ID"])) { 
                    $WG_TOKEN_KEY = $_COOKIE["WG_CLIENT_ID"];  // client_id를 token_key로 사용중
                } else {
                    $WG_TOKEN_KEY = WG_GetRandomString(10);
                    setcookie ("WG_CLIENT_ID", $WG_TOKEN_KEY, time() + (86400 * 7), "/"); // log
                }
                if(isset($_COOKIE["WG_WAS_IP"])) { 
                    $WG_WAS_IP    = $_COOKIE["WG_WAS_IP"]; 
                }

                    

                if($WG_TOKEN_NO !== null && $WG_TOKEN_NO !=="" && $WG_TOKEN_KEY !== null && $WG_TOKEN_KEY !== "" && $WG_WAS_IP !== null && $WG_WAS_IP !== "")
                {
                    // 대기표 Validation(checkout api call)
                    $apiUrl = "http://" . $WG_WAS_IP . "/?ServiceId=" . $WG_SERVICE_ID . "&GateId=" . $WG_GATE_ID . "&Action=OUT&TokenNo=" . $WG_TOKEN_NO . "&TokenKey=" . $WG_TOKEN_KEY . "&IsLoadTest=" . $WG_IS_LOADTEST;
                    
                    $responseText = file_get_contents($apiUrl);
                    if($responseText != null && $responseText != "" && strpos($responseText, "\"ResultCode\":0") !== false)
                    {
                        $WG_IS_CHECKOUT_OK = true;
                    } 
                }
            }
        }
        catch(Exception $e) 
        {
            $WG_TRACE .= "ERROR:".$e->getMessage().", ";
            // ignore & goto next
        }

        /******************************************************************************
        STEP-3 : 대기표가 정상이 아니면(=체크아웃실패) 신규접속자로 간주하고 대기열 표시여부 판단
                 WG_GATE_SERVERS 서버 중 임의의 서버에 API 호출
        *******************************************************************************/


        $WG_IS_NEED_TO_WAIT = false;
        if($WG_IS_CHECKOUT_OK == false) 
        {
            $WG_TRACE .= "STEP3, ";
            $lineText="";
            $receiveText="";
            $serverCount = count($WG_GATE_SERVERS);
            $drawResult  = rand(0, $serverCount-1); // 1차대기열서버 : 임의의 대기열 서버
            
            // Fail-over를 위해 최대 3차까지 시도
            for($i = 0; $i < $WG_MAX_TRY_COUNT; $i++)
            {
                try
                {
                    $serverIp = $WG_GATE_SERVERS[($drawResult++)%($serverCount)];
                    $apiUrl =  "http://" . $serverIp . "/?ServiceId=" . $WG_SERVICE_ID . "&GateId=" . $WG_GATE_ID . "&Action=CHECK" . "&TokenKey=" . $WG_TOKEN_KEY . "&IsLoadTest=" . $WG_IS_LOADTEST;
                    $responseText = file_get_contents($apiUrl);
                    if($responseText == null || $responseText == "") { continue; }  

                
                    // 현재 대기자가 있으면 응답문자열에 "WAIT"가 포함, 대기자 수가 없으면 "PASS"가 포함됨
                    if(strpos($responseText, "WAIT") !== false) 
                    {
                        $WG_TRACE .=  $apiUrl . "--> WAIT, ";
                        $WG_IS_NEED_TO_WAIT = true;
                        break; 
                    } 
                    else  // PASS (대기가 없는 경우)
                    {  
                        $WG_TRACE .=  $apiUrl . "--> PASS, ";
                        $WG_IS_NEED_TO_WAIT = false;
                        break; 
                    }
                }
                catch(Exception $e)  { 
                    $WG_TRACE .= "ERROR:".$e->getMessage().", ";
                    // try next
                }
            }
        } 

        // Timeout 설정 복구
        ini_set("default_socket_timeout", $WG_SOCKET_TIMEOUT);

        $result = true;
        if($WG_IS_CHECKOUT_OK || !$WG_IS_NEED_TO_WAIT)
        {
            $result = false;
            $WG_TRACE .= "return:false, ";
        }
        else 
        {
            $result = true;
            $WG_TRACE .= "return:true, ";
        }

        
        // write cookie for trace
        setcookie ("WG_VERSION", $WG_VERSION, time() + (86400 * 7), "/"); 
        setcookie ("WG_TIME", date("Ymd-His"), time() + (86400 * 7), "/"); 
        setcookie ("WG_TRACE", $WG_TRACE, time() + (86400 * 7), "/");
        
        return $result;

    }

    function WG_GetWaitingUi($service_id, $gate_id)
    {
        // template html
		$html = "<!DOCTYPE html>\r\n"
                . "<html>\r\n"
                . "<head>\r\n"
                . "    <meta http-equiv='X-UA-Compatible' content='IE=edge'>\r\n"
                . "    <meta charset='utf-8'>\r\n"
                . "    <meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, user-scalable=no'>\r\n"
                . "    <title></title>\r\n"
                . "    <style> html, body {margin:0; padding:0; overflow-x:hidden; overflow-y:hidden; width:100%; height:100%;} </style> "
                . "</head>\r\n"
                . "<body>\r\n"
                . "    <div id='wg-body-wrapper'></div>\r\n"
                . "    <link href='https://cdn.devy.kr/WG_SERVICE_ID/css/webgate.css?v=210611' rel='stylesheet'>\r\n"
                . "    <script type='text/javascript' src='https://cdn.devy.kr/WG_SERVICE_ID/js/webgate.js?v=210611'></script>\r\n"
                . "    <script>\r\n"
                . "        window.addEventListener('load', function () {\r\n"
                . "            WG_StartWebGate('WG_GATE_ID', window.location.href); //reload \r\n"
                . "        });\r\n"
                . "    </script>\r\n"
                . "</body>\r\n"
                . "</html>\r\n";
				 
        // replace
        $html =  str_replace("WG_SERVICE_ID", $service_id, $html); 
        $html =  str_replace("WG_GATE_ID"   , $gate_id   , $html); 

        return $html;
    }


    function WG_GetRandomString($length=10) 
    {
        $characters = '0123456789ABCDEF';
        $randstring = '';
        for ($i = 0; $i < $length; $i++) {
            $randstring .= $characters[rand(0, strlen($characters)-1)];
        }
        return $randstring;
    }
?>