<%
    '/* 
    '* ==============================================================================================
    '* 메가펜스 유량제어서비스 Backend Library for ASP / V.22.10.30
    '* 이 라이브러리는 메가펜스 서비스 계약 및 테스트(POC) 고객에게 제공됩니다.
    '* 오류조치 및 개선을 목적으로 자유롭게 수정 가능하며 수정된 내용은 반드시 공급처에 통보해야 합니다.
    '* 허가된 고객 및 환경 이외의 열람, 복사, 배포, 수정, 실행, 테스트 등 일체의 이용을 금합니다.
    '* 작성자 : ysd@devy.co.kr
    '* All rights reserved to DEVY / https://devy.kr
    '* ==============================================================================================
    '*/
    
    FUNCTION WG_IsNeedToWaiting(WG_SERVICE_ID, WG_GATE_ID)
        Dim WG_VERSION         
        Dim WG_MAX_TRY_COUNT   
        Dim WG_IS_CHECKOUT_OK  
        Dim WG_GATE_SERVER_MAX 
        Dim WG_GATE_SERVERS()    
        Dim WG_TOKEN_NO        
        Dim WG_TOKEN_KEY       
        Dim WG_WAS_IP          
        Dim WG_TRACE           
        Dim WG_IS_LOADTEST, WG_IS_LOADTEST_PARAM     
        Dim WG_CLIENT_IP


        WG_VERSION              = "V.23.07.21"
        WG_MAX_TRY_COUNT        = 3                            '[fixed] failover api retry count
        WG_IS_CHECKOUT_OK       = False                        '[fixed] 대기를 완료한 정상 대기표 여부 (true : 대기완료한 정상 대기표, false : 정상대기표 아님)
        WG_GATE_SERVER_MAX      = 6                            '[fixed] was dns record count
        WG_TOKEN_NO             = ""                           '대기표 ID
        WG_TOKEN_KEY            = ""                           '대기표 key
        WG_WAS_IP               = ""                           '대기표 발급서버
        WG_TRACE                = ""                           'TRACE 정보 (쿠키응답)
        WG_IS_LOADTEST          = "N"                          'jmeter 등으로 발생시킨 요청인지 여부
        
        'get client ip
        WG_CLIENT_IP = Request.ServerVariables("REMOTE_ADDR")
        If IsNull(WG_CLIENT_IP) Or Len(Trim(WG_CLIENT_IP)) = 0 Then
            WG_CLIENT_IP = "N/A"
        End If
           


        'init gate server list 
        ReDim WG_GATE_SERVERS(WG_GATE_SERVER_MAX)
        Dim i
        For i = 0 To WG_GATE_SERVER_MAX -1 Step 1   
            WG_GATE_SERVERS(i) = WG_SERVICE_ID & "-" & i & ".devy.kr"   
        Next 
        
	    'JMeter 등에서 부하테스트(LoadTest)용으로 호출된 경우를 위한 처리 (부하발생 시 URL에 IsLoadTest=Y parameter 추가해야 합니다)
        WG_IS_LOADTEST_PARAM = Request.QueryString("IsLoadTest")
        If Not IsNull(WG_IS_LOADTEST_PARAM) And Not IsEmpty(WG_IS_LOADTEST_PARAM) Then
            WG_IS_LOADTEST = WG_IS_LOADTEST_PARAM
        End If

        'API Call Timeout : 2초 무응답 시 장애 간주
        Dim ApiUrl, ResponseText
        Dim XmlHttp : Set XmlHttp = Server.CreateObject("Msxml2.ServerXMLHTTP.3.0")


        '******************************************************************************
        'STEP-1 : URL Prameter로 대기표 검증 (CDN Landing 방식을 이용하는 경우에 해당)
        '*******************************************************************************
        'Try 시작
        On Error Resume Next   
            WG_TRACE = WG_TRACE & "STEP1:"
            'WG_TOKEN paramter를 ','로 분리 및 분리된 개수 체크
            Dim TokenParam
            TokenParam = Request.QueryString("WG_TOKEN")
            If Not IsNull(TokenParam) Then
                Dim TokenValues
                TokenValues = Split(TokenParam, ",")
                
                If Ubound(TokenValues) = Ubound(Split("GATE_ID,TOKEN_NO,TOKEN_KEY,WAS_IP",",")) Then
                    Dim TEMP_TOKEN_NO 
                    Dim TEMP_TOKEN_KEY
                    Dim TEMP_WAS_IP   

                    TEMP_TOKEN_NO  = TokenValues(1)
                    TEMP_TOKEN_KEY = TokenValues(2)
                    TEMP_WAS_IP    = TokenValues(3)
                    'Response.Write( "WG_TOKEN_NO:" & TEMP_TOKEN_NO & ", WG_TOKEN_KEY:" & TEMP_TOKEN_KEY & ", WG_WAS_IP:" & TEMP_WAS_IP)                
                    
                    '대기표 Validation(checkout api call)
                    ApiUrl =  "https://" & TEMP_WAS_IP & "/?ServiceId=" & WG_SERVICE_ID & "&GateId=" & WG_GATE_ID & "&Action=OUT&TokenNo=" & TEMP_TOKEN_NO & "&TokenKey=" & TEMP_TOKEN_KEY & "&IsLoadTest=" & WG_IS_LOADTEST
                    'WG_TRACE = WG_TRACE & "API_URL:" & ApiUrl & ", "

                    ' Call API
                    XmlHttp.SetTimeouts 20000, 20000, 20000, 20000
                    ResponseText = WG_CallApi(ApiUrl, XmlHttp)
                    If Not IsNull(ResponseText) And Not IsEmpty(ResponseText) And InStr(ResponseText, """ResultCode"":0") Then
                        WG_IS_CHECKOUT_OK = True
                        WG_TRACE = WG_TRACE & "OK,"

                        ' set cookie from WG_TOKEN param
	                    WG_WriteCookie "WG_CLIENT_ID", WG_TOKEN_KEY
	                    WG_WriteCookie "WG_WAS_IP", WG_WAS_IP
	                    WG_WriteCookie "WG_TOKEN_NO", WG_TOKEN_NO


                    Else
                        WG_TRACE = WG_TRACE & "FAIL,"
                    End If
                Else
                    WG_TRACE = WG_TRACE & "SKIP1,"
                End If
            Else
                WG_TRACE = WG_TRACE & "SKIP2,"
            End If
        'Catch
        If Err <> 0 Then   
            WG_TRACE = WG_TRACE & "ERROR:" & Err.Description & ","
            'ignore & goto next
        End If
        'Error Clear
        On Error GoTo 0 

        
    


        '******************************************************************************
        'STEP-2 : Cookie로 대기표 검증 (CDN Landing 방식 이외의 일반적인 방식에 해당)
        '*******************************************************************************
        'Try 시작
        On Error Resume Next   
            WG_TRACE = WG_TRACE & "→STEP2:"

            WG_TOKEN_NO  = Request.Cookies("WG_TOKEN_NO")
            WG_TOKEN_KEY = Request.Cookies("WG_CLIENT_ID")
            WG_WAS_IP    = Request.Cookies("WG_WAS_IP")
        

            If IsEmpty(WG_TOKEN_KEY) OR Len(WG_TOKEN_KEY) = 0 Then
                WG_TOKEN_KEY = RandomString(8)
            End If

            If Not WG_IS_CHECKOUT_OK Then

                Dim cookieGateId
                cookieGateId = Request.Cookies("WG_GATE_ID") 
                
                
                If Len(WG_TOKEN_NO) > 0 And Len(WG_TOKEN_KEY) > 0 And Len(WG_WAS_IP) > 0 And Len(WG_GATE_ID) > 0 And Len(cookieGateId) > 0 Then
                    If StrComp(WG_GATE_ID, cookieGateId) = 0 Then
                        '대기표 Validation(checkout api call)
                        ApiUrl =  "https://" & WG_WAS_IP & "/?ServiceId=" & WG_SERVICE_ID & "&GateId=" & WG_GATE_ID & "&Action=OUT&TokenNo=" & WG_TOKEN_NO & "&TokenKey=" & WG_TOKEN_KEY & "&IsLoadTest=" & WG_IS_LOADTEST

                        ' Call API
                        XmlHttp.SetTimeouts 20000, 20000, 20000, 20000
                        ResponseText = WG_CallApi(ApiUrl, XmlHttp)
                        If Not IsNull(ResponseText) And Not IsEmpty(ResponseText) And InStr(ResponseText, """ResultCode"":0") Then
                            WG_IS_CHECKOUT_OK = True
                            WG_TRACE = WG_TRACE & "OK,"
                        Else
                            WG_TRACE = WG_TRACE & "FAIL,"
                        End If
                    Else
                        WG_TRACE = WG_TRACE & "SKIP1,"
                    End If
                Else
                    WG_TRACE = WG_TRACE & "SKIP2,"
                End If
            Else
                WG_TRACE = WG_TRACE & "SKIP3,"
            End If
        'Catch
        If Err <> 0 Then   
            WG_TRACE = WG_TRACE & "ERROR:" & Err.Description & ","
            'ignore & goto next
        End If
        'Error Clear
        On Error GoTo 0 

        
        If WG_IS_CHECKOUT_OK Then
            WG_IsNeedToWaiting = False
        Else
            WG_IsNeedToWaiting = True
        End If
        WG_TRACE = WG_TRACE & "→Returns:" & Cstr(WG_IsNeedToWaiting)
        


        'Cookie Write for trace
        On Error Resume Next   
            WG_WriteCookie "WG_TRACE", WG_TRACE
            WG_WriteCookie "WG_VER_BACKEND", WG_VERSION
            Dim YmdHms : YmdHms = WG_UtcTimeFromat(Now()) 
            WG_WriteCookie "WG_TIME", YmdHms
            WG_WriteCookie "WG_CLIENT_IP", WG_CLIENT_IP
            WG_WriteCookie "WG_WAS_IP", WG_WAS_IP
            WG_WriteCookie "WG_GATE_ID", WG_GATE_ID
            WG_WriteCookie "WG_CLIENT_ID", WG_TOKEN_KEY

        'Catch
        If Err <> 0 Then   
            WG_TRACE = WG_TRACE & "ERROR:" & Err.Description
            'ignore & goto next
        End If
        'Error Clear
        On Error GoTo 0 

    END FUNCTION
    
    
    FUNCTION WG_GetWaitingUi(WG_SERVICE_ID, WG_GATE_ID)
        'template html
        Dim html
		Html = "<!DOCTYPE html>"                                                                                                                        & vbCrLf &_
                "<html>"                                                                                                                                & vbCrLf &_
                "<head>"                                                                                                                                & vbCrLf &_
                "    <meta http-equiv='X-UA-Compatible' content='IE=edge'>"                                                                             & vbCrLf &_
                "    <meta charset='utf-8'>"                                                                                                            & vbCrLf &_
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, user-scalable=no'>"    & vbCrLf &_
                "    <title></title>"                                                                                                                   & vbCrLf &_
                "    <style> html, body {margin:0; padding:0; overflow-x:hidden; overflow-y:hidden; width:100%; height:100%;} </style> "                & vbCrLf &_
                "</head>"                                                                                                                               & vbCrLf &_
                "<body>"                                                                                                                                & vbCrLf &_
                "    <link href='https://cdn2.devy.kr/WG_SERVICE_ID/css/webgate.css?v=210611' rel='stylesheet'>"                                        & vbCrLf &_
                "    <script type='text/javascript' src='https://cdn2.devy.kr/WG_SERVICE_ID/js/webgate.js?v=210611'></script>"                          & vbCrLf &_
                "    <script>"                                                                                                                          & vbCrLf &_
                "        window.addEventListener('load', function () {"                                                                                 & vbCrLf &_
                "            WG_StartWebGate('WG_GATE_ID', window.location.href, 'BACKEND'); //reload "                                                 & vbCrLf &_
                "        });"                                                                                                                           & vbCrLf &_
                "    </script>"                                                                                                                         & vbCrLf &_
                "</body>"                                                                                                                               & vbCrLf &_
                "</html>"                                                                                                                               
				                                                                                                                                            
        'replace                                                                                                                                          
        Html =  Replace(Html, "WG_SERVICE_ID", WG_SERVICE_ID)                                                                                              
        Html =  Replace(Html, "WG_GATE_ID", WG_GATE_ID)
                                                                                                                                                            
        WG_GetWaitingUi = Html                                                                                                                              
    END FUNCTION                                                                                                                                            
    

    ' HTTP API Call
    Function WG_CallApi(Url, XmlHttp)
        XmlHttp.Open "GET", Url, False
        XmlHttp.Send

        If XmlHttp.Status = 200 Then
            WG_CallApi = XmlHttp.ResponseText
        End If
    End Function

    Function WG_WriteCookie(Key,Value)
        Dim Expire : Expire = FormatDateTime(Date()+7, 2) & " " & FormatDateTime(Date()+1, 4) 
        Response.AddHeader "Set-Cookie", Key & "=" & Server.URLEncode(Value) & ";Path=/;Expires=" & Expire
    End Function


    Function WG_UtcTimeFromat(dt)
        WG_UtcTimeFromat = Year(dt) & "-" & Month(dt) & "-" & Day(dt) & "T" & Hour(dt) & ":" & Minute(dt) & ":" & second(dt) & "+09:00" '서울(KST)로 가정    
    End Function


    Function RandomString(StrLen)
        Dim ReturnValue, i
        Const CharPool= "12345678ABCDEFGHJKLMNPQRSTWXYZ"
        Randomize
        For i = 1 to StrLen
            ReturnValue = ReturnValue & Mid(CharPool,Int((Len(CharPool)*Rnd)+1),1)
        Next
        RandomString = ReturnValue
    End Function

%>