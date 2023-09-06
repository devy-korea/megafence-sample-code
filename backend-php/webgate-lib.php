<?php
    /* 
    * ==============================================================================================
    * 메가펜스 유량제어서비스 Backend Library for PHP / 23.09.03
    * 이 라이브러리는 메가펜스 서비스 계약 및 테스트(POC) 고객에게 제공됩니다.
    * 오류조치 및 개선을 목적으로 자유롭게 수정 가능하며 수정된 내용은 반드시 공급처에 통보해야 합니다.
    * 허가된 고객 및 환경 이외의 열람, 복사, 배포, 수정, 실행, 테스트 등 일체의 이용을 금합니다.
    * 작성자 : ysd@devy.co.kr
    * All rights reserved to DEVY / https://devy.kr
    * ==============================================================================================
    */



    /*
    대기여부를 판단
    */
    function WG_IsNeedToWaiting($service_id, $gate_id)
    {

        $WG_VERSION         = "23.09.03";
        $WG_SERVICE_ID      = $service_id;            
        $WG_GATE_ID         = $gate_id;              
        $WG_MAX_TRY_COUNT   = 3;            // [fixed] failover api retry count
        $WG_IS_CHECKOUT_OK  = false;        // [fixed] 대기를 완료한 정상 대기표 여부 (true : 대기완료한 정상 대기표, false : 정상대기표 아님)
        $WG_GATE_SERVER_MAX = 6;            // [fixed] was dns record count
        $WG_GATE_SERVERS    = array ();     // [fixed] 대기표 발급서버 Address List
        $WG_TOKEN_NO        = "";           // 대기표 ID
        $WG_TOKEN_KEY       = "";           // 대기표 key
        $WG_WAS_IP          = "";           // 대기표 발급서버
        $WG_TRACE           = "";           // TRACE 정보 (쿠키응답)
        $WG_IS_LOADTEST     = "N";          // jmeter 등으로 발생시킨 요청인지 여부
        $WG_CLIENT_IP       = "";           // 단말 IP (운영자 IP 판단용)
		$WG_COOKIE_DOMAIN   = "";

		
        /* get clipent ip */
        $WG_CLIENT_IP = $_SERVER["REMOTE_ADDR"];
        if(empty($WG_CLIENT_IP))
        {
            $WG_CLIENT_IP = "N/A";
        }


        /* init gate server list */
        for($i=0; $i < $WG_GATE_SERVER_MAX; $i++)
        {
            array_push($WG_GATE_SERVERS, $service_id."-".$i.".devy.kr");
        }

	    /*
        JMeter 등에서 부하테스트(LoadTest)용으로 호출된 경우를 위한 처리 (부하발생 시 URL에 IsLoadTest=Y parameter 추가해야 합니다)
	    */
        if (isset($_GET["IsLoadTest"]))
        {
            $WG_IS_LOADTEST = $_GET["IsLoadTest"];
        }
        if($WG_IS_LOADTEST != null && $WG_IS_LOADTEST == "Y" )
        {
            $WG_IS_LOADTEST = "Y";
        }

        // Timeout 제어 (2초이내 무응답 장애간주)
        $WG_SOCKET_TIMEOUT = ini_get("default_socket_timeout");


        /******************************************************************************
        STEP-1 : URL Prameter로 대기표 검증 (CDN Landing 방식을 이용하는 경우에 해당)
        *******************************************************************************/
        try 
        {
            ini_set("default_socket_timeout", 30);

            $WG_TRACE .= "STEP1:";
            if(isset($_GET["WG_TOKEN"])) 
            {

                // WG_TOKEN paramter를 ','로 분리 및 분리된 개수 체크
                $parameterValues = explode(",", $_GET["WG_TOKEN"]);
                if (count($parameterValues) == count(explode(",", "GATE_ID,TOKEN_NO,TOKEN_KEY,WAS_IP")))
                {
                    // WG_TOKEN parameter에 세팅된 값 GET
                    $paramGateId    = $parameterValues[0];
                    $WG_TOKEN_NO    = $parameterValues[1];
                    $WG_TOKEN_KEY   = $parameterValues[2];
                    $WG_WAS_IP      = $parameterValues[3];

                    if( $WG_TOKEN_NO     !== null   && $WG_TOKEN_NO  !=="" 
                        && $WG_TOKEN_KEY !== null   && $WG_TOKEN_KEY !== "" 
                        && $WG_WAS_IP    !== null   && $WG_WAS_IP    !== ""
                        && $paramGateId  !== null   && strcmp($paramGateId, $WG_GATE_ID) == 0 )
                    {
                        // 대기표 Validation(checkout api call)
                        $apiUrl = "https://" . $WG_WAS_IP . "/?ServiceId=" . $WG_SERVICE_ID . "&GateId=" . $WG_GATE_ID . "&Action=OUT&TokenNo=" . $WG_TOKEN_NO . "&TokenKey=" . $WG_TOKEN_KEY . "&IsLoadTest=" . $WG_IS_LOADTEST;

                        $responseText = file_get_contents($apiUrl);
                        if($responseText != null && $responseText != "" && strpos($responseText, "\"ResultCode\":0") !== false)
                        {
                            $WG_IS_CHECKOUT_OK = true;
                            $WG_TRACE .= "OK";

	                        // set cookie from WG_TOKEN param
	                        WG_WriteCookie("WG_CLIENT_ID", $WG_TOKEN_KEY);
	                        WG_WriteCookie("WG_WAS_IP", $WG_WAS_IP);
	                        WG_WriteCookie("WG_TOKEN_NO", $WG_TOKEN_NO);
                        }
                        else {
                            $WG_TRACE .= "FAIL,";
                        }
                    }
                    else {
                        $WG_TRACE .= "SKIP1,";
                    }
                } else {
                    $WG_TRACE .= "SKIP2,";
                }
            } else {
                $WG_TRACE .= "SKIP3,";
            } 
        }
        catch(Exception $e) 
        {
            $WG_TRACE .= "ERROR:".$e->getMessage().",";
            // ignore & goto next
        }

        /******************************************************************************
        STEP-2 : Cookie로 대기표 검증 (CDN Landing 방식 이외의 일반적인 방식에 해당)
        *******************************************************************************/
        try 
        {
            ini_set("default_socket_timeout", 30);

            $WG_TRACE .= "→STEP2:";
            if($WG_IS_CHECKOUT_OK == false)
            {
                $cookieGateId = WG_ReadCookie("WG_GATE_ID");
                $WG_TOKEN_NO  = WG_ReadCookie("WG_TOKEN_NO"); 
                
                if(isset($_GET["WG_CLIENT_ID"]) && strlen($_GET["WG_CLIENT_ID"]))
                {
                    $WG_TOKEN_KEY = $_GET["WG_CLIENT_ID"];  // cdn에서 post로 이동하는 경우 대응 (rankingdak.com)
                }
                else {
                    $WG_TOKEN_KEY = WG_ReadCookie("WG_CLIENT_ID");  // client_id를 token_key로 사용중
                }


                if ($WG_TOKEN_KEY == ""){
                    $WG_TOKEN_KEY = WG_GetRandomString(8);
                    WG_WriteCookie("WG_CLIENT_ID", $WG_TOKEN_KEY);
                }

                $WG_WAS_IP = WG_ReadCookie("WG_WAS_IP");

                if(isset($WG_TOKEN_NO) && strlen($WG_TOKEN_NO) > 0 && 
                   isset($WG_TOKEN_KEY) && strlen($WG_TOKEN_KEY) > 0 && 
                   isset($WG_WAS_IP) && strlen($WG_WAS_IP) > 0 && isset($cookieGateId) && strlen($cookieGateId) > 0  && strcmp($cookieGateId,$WG_GATE_ID) == 0)
                {
                    // 대기표 Validation(checkout api call)
                    $apiUrl = "https://" . $WG_WAS_IP . "/?ServiceId=" . $WG_SERVICE_ID . "&GateId=" . $WG_GATE_ID . "&Action=OUT&TokenNo=" . $WG_TOKEN_NO . "&TokenKey=" . $WG_TOKEN_KEY . "&IsLoadTest=" . $WG_IS_LOADTEST;
                    //$WG_TRACE .=  $apiUrl.",";
                    $responseText = file_get_contents($apiUrl);
                    if($responseText != null && $responseText != "" && strpos($responseText, "\"ResultCode\":0") !== false)
                    {
                        $WG_IS_CHECKOUT_OK = true;
                        $WG_TRACE .= "OK,";
                    } else {
                        $WG_TRACE .= "FAIL,";
                    }
                }
                else {
                    $WG_TRACE .= "SKIP1,";
                }
            }
            else {
                $WG_TRACE .= "SKIP2";
            }
        }
        catch(Exception $e) 
        {
            $WG_TRACE .= "ERROR:".$e->getMessage().",";
            // ignore & goto next
        }


        /******************************************************************************
        STEP-3 : 대기표가 정상이 아니면(=체크아웃실패) 신규접속자로 간주하고 대기열 표시여부 판단
                 WG_GATE_SERVERS 서버 중 임의의 서버에 API 호출
        *******************************************************************************/
        $WG_TRACE .= "→STEP3:";

        $WG_IS_NEED_TO_WAIT = false;
        $tryCount = 0;
        if($WG_IS_CHECKOUT_OK == false) 
        {
            $lineText="";
            $receiveText="";
            $serverCount = count($WG_GATE_SERVERS);
            $drawResult  = rand(0, $serverCount-1); // 1차대기열서버 : 임의의 대기열 서버
            
            // Fail-over를 위해 최대 3차까지 시도
            for($tryCount = 0; $tryCount < $WG_MAX_TRY_COUNT; $tryCount++)
            {
                try
                {
                    ini_set("default_socket_timeout", 5*($tryCount+1));


                    if($tryCount == 0 && strlen($WG_WAS_IP) > 0)
                    {
						$serverIp = $WG_WAS_IP;
                    } else {
                        $serverIp = $WG_GATE_SERVERS[($drawResult++)%($serverCount)];
                    }

                    
                    $apiUrl =  "https://" . $serverIp . "/?ServiceId=" . $WG_SERVICE_ID . "&GateId=" . $WG_GATE_ID . "&Action=CHECK" . "&ClientIp=" . $WG_CLIENT_IP . "&TokenKey=" . $WG_TOKEN_KEY . "&IsLoadTest=" . $WG_IS_LOADTEST;
                    
                    //$WG_TRACE .=  $apiUrl.",";
                    $responseText = file_get_contents($apiUrl);
                    if($responseText == null || $responseText == "") { continue; }  

                
                    // 현재 대기자가 있으면 응답문자열에 "WAIT"가 포함, 대기자 수가 없으면 "PASS"가 포함됨
                    if(strpos($responseText, "WAIT") !== false) 
                    {
                        $WG_TRACE .=  "WAIT,";
                        $WG_IS_NEED_TO_WAIT = true;
                        break; 
                    } 
                    else if(strpos($responseText, "PASS") !== false)  
                    {  
                        $WG_TRACE .=  "PASS,";
                        $WG_IS_NEED_TO_WAIT = false;
                        break; 
                    }
                    else {
						$WG_TRACE .=  "FAIL:" . $responseText . ",";
                    }
                }
                catch(Exception $e)  { 
                    $WG_TRACE .= "ERROR:".$e->getMessage().",";
                    // try next
                }
            }
        }
        else {
            $WG_TRACE .= "SKIP";
        }
        $WG_TRACE .= "TryCount:".$tryCount.",";

        // Timeout 설정 복구
        ini_set("default_socket_timeout", $WG_SOCKET_TIMEOUT);

        $result = true;
        if($WG_IS_CHECKOUT_OK || !$WG_IS_NEED_TO_WAIT)
        {
            $result = false;
        }
        else 
        {
            $result = true;
        }
        $WG_TRACE .= "→returns:".$result;

        
        // write cookie for trace
		WG_WriteCookie ("WG_MOD_BACKEND", "PHP"); 
        WG_WriteCookie ("WG_VER_BACKEND", $WG_VERSION); 
        WG_WriteCookie ("WG_TIME", date("c")); 
        WG_WriteCookie ("WG_TRACE", $WG_TRACE);
		WG_WriteCookie ("WG_CLIENT_IP", $WG_CLIENT_IP);
		WG_WriteCookie ("WG_GATE_ID", $WG_GATE_ID);
		WG_WriteCookie ("WG_WAS_IP", $WG_WAS_IP);

		
        
        return $result;

    }

    function WG_GetWaitingUi($service_id, $gate_id)
    {
        $versionTag = date("YmdHi"); // 1분 캐시용 url param

        // template html
		$html = "<!DOCTYPE html>\r\n"
                . "<html>\r\n"
                . "<head>\r\n"
                . "    <meta http-equiv='X-UA-Compatible' content='IE=edge'>\r\n"
                . "    <meta charset='utf-8'>\r\n"
                . "    <meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, user-scalable=no'>\r\n"
                . "    <title></title>\r\n"
                . "    <style> html, body {margin:0; padding:0; overflow-x:hidden; overflow-y:hidden; width:100%; height:100%;} </style> "
                . "    <link href='https://cdn2.devy.kr/WG_SERVICE_ID/css/webgate.css?v=" . $versionTag ."' rel='stylesheet'>\r\n"
                . "</head>\r\n"
                . "<body>\r\n"
                //. "    <div id='wg-body-wrapper'></div>\r\n"
                . "    <script type='text/javascript' src='https://cdn2.devy.kr/WG_SERVICE_ID/js/webgate.js?v=" . $versionTag ."'></script>\r\n"
                . "    <script>\r\n"
                . "        window.addEventListener('load', function () {\r\n"
                . "            WG_StartWebGate('WG_GATE_ID', window.location.href, 'BACKEND'); //reload \r\n"
                . "        });\r\n"
                . "    </script>\r\n"
                . "</body>\r\n"
                . "</html>\r\n";
				 
        // replace
        $html =  str_replace("WG_SERVICE_ID", $service_id, $html); 
        $html =  str_replace("WG_GATE_ID"   , $gate_id   , $html); 

        return $html;
    }


    function WG_GetRandomString($length=8) 
    {
        //$characters = '0123456789ABCDEF';
        $characters = '12345678ABCDEFGHJKLMNPQRSTWXYZ';
        $randstring = '';
        for ($i = 0; $i < $length; $i++) {
            $randstring .= $characters[rand(0, strlen($characters)-1)];
        }
        return $randstring;
    }

    function WG_ReadCookie($key) 
    {
        if(isset($_COOKIE[$key])) { 
            return $_COOKIE[$key];  
        }
        else {
            return "";
        }
    }

    function WG_WriteCookie($key, $value)
    {
        setcookie ($key, $value, time() + (86400 * 1), "/"); // default

    }

?>

