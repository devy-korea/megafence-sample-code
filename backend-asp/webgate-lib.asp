<%
    '/* 
    '* ==============================================================================================
    '* 메가펜스 유량제어서비스 Backend Library for ASP / V.23.10.06
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


        WG_VERSION              = "25.1.914"
        WG_MAX_TRY_COUNT        = 3                            '[fixed] failover api retry count
        WG_IS_CHECKOUT_OK       = False                        '[fixed] 대기를 완료한 정상 대기표 여부 (true : 대기완료한 정상 대기표, false : 정상대기표 아님)
        WG_GATE_SERVER_MAX      = 6                            '[fixed] was dns record count
        WG_TOKEN_NO             = ""                           '대기표 ID
        WG_TOKEN_KEY            = ""                           '대기표 key
        WG_WAS_IP               = ""                           '대기표 발급서버
        WG_TRACE                = ""                           'TRACE 정보 (쿠키응답)
        WG_IS_LOADTEST          = "N"                          'jmeter 등으로 발생시킨 요청인지 여부
        
        'get client ip
        WG_CLIENT_IP = GetClientIP()


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
        Dim XmlHttp : Set XmlHttp = Server.CreateObject("MSXML2.ServerXMLHTTP.6.0")

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

                    
                if Right(LCase(TEMP_WAS_IP), Len(".devy.kr")) = ".devy.kr" Then 'Check SSRF
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
                    WG_TRACE = WG_TRACE & "FAIL(SSRF),"
                End if
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

        'CHECK SSRF
        'If Not IsEmpty(WG_WAS_IP) And Right(Lcase(WG_WAS_IP), Len(".devy.kr")) =  ".devy.kr" Then 
        '    WG_WAS_IP = ""
        'End if

        

        If IsEmpty(WG_TOKEN_KEY) OR Len(WG_TOKEN_KEY) = 0 Then
            WG_TOKEN_KEY = WG_RandomString(8)
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

        
        '******************************************************************************
        'STEP-3 : 대기표가 정상이 아니면(=체크아웃실패) 신규접속자로 간주하고 대기열 표시여부 판단
        '         WG_GATE_SERVERS 서버 중 임의의 서버에 API 호출
        '*******************************************************************************/
        WG_TRACE = WG_TRACE & "→STEP3:"
        Dim WG_IS_NEED_TO_WAIT 
        WG_IS_NEED_TO_WAIT = False

        On Error Resume Next   
        If Not WG_IS_CHECKOUT_OK Then
            Dim LineText, ReceiveText, DrawResult
            Dim TryCount

            Randomize
            DrawResult = Int(WG_GATE_SERVER_MAX * Rnd + 0)

            TryCount = 0
            For TryCount = 0 To WG_MAX_TRY_COUNT Step 1
                'Try 시작
                On Error Resume Next   
                    DrawResult = DrawResult + 1
                        
                    If TryCount = 0 And Len(WG_WAS_IP) > 0 Then
                        'use last was ip from cookie when first time
                    Else
                        WG_WAS_IP = WG_GATE_SERVERS(DrawResult Mod WG_GATE_SERVER_MAX)
                    End If
                        

                    ApiUrl =  "https://" & WG_WAS_IP & "/?ServiceId=" & WG_SERVICE_ID & "&GateId=" & WG_GATE_ID & "&Action=CHECK" & "&ClientIp=" & WG_CLIENT_IP  & "&TokenKey=" & WG_TOKEN_KEY & "&IsLoadTest=" & WG_IS_LOADTEST
                    ' Call API
                    XmlHttp.SetTimeouts 5*(TryCount+1), 5*(TryCount+1), 5*(TryCount+1), 5*(TryCount+1)
                    ResponseText = WG_CallApi(ApiUrl, XmlHttp)
                    If Not IsNull(ResponseText) And Not IsEmpty(ResponseText) Then
                        If InStr(ResponseText, "WAIT") Then
                            WG_TRACE =  WG_TRACE & "WAIT,"
                            WG_IS_NEED_TO_WAIT = True
                            Exit For
                        ElseIf InStr(ResponseText, "PASS") Then ' PASS
                            WG_TRACE =  WG_TRACE & "PASS,"
                            WG_IS_NEED_TO_WAIT = False
                            Exit For
                        Else 
                            WG_TRACE =  WG_TRACE & "Fail:" & ResponseText & ","
                        End If
                    Else
                        WG_TRACE = WG_TRACE & "FAIL,"                    
                    End If
                'Catch
                If Err <> 0 Then   
                    WG_TRACE = WG_TRACE & "ERROR:" & Err.Description & ","
                    'ignore & goto next
                End If
                'Error Clear
                On Error GoTo 0 
            Next
        Else
            WG_TRACE = WG_TRACE & "SKIP,"
        End If
        WG_TRACE = WG_TRACE & "TryCount:" & Cstr(TryCount) & ","

        If WG_IS_CHECKOUT_OK Or Not WG_IS_NEED_TO_WAIT Then
            WG_IsNeedToWaiting = False
        Else
            WG_IsNeedToWaiting = True
        End If
        WG_TRACE = WG_TRACE & "→Returns:" & Cstr(WG_IsNeedToWaiting)

        'Catch
        If Err <> 0 Then   
            WG_TRACE = WG_TRACE & "ERROR:" & Err.Description
            'ignore & goto next
        End If
        'Error Clear
        On Error GoTo 0 


        'Cookie Write for trace
        On Error Resume Next   
        WG_WriteCookie "WG_TRACE", WG_TRACE
        WG_WriteCookie "WG_MOD_BACKEND", "ASP"
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
        Dim VersionTag, Html
        VersionTag = Year(Now()) & Month(Now()) & Day(Now()) & Hour(Now()) & "00" '1시간 단위(yyyyMMddHH00)

		Html = "<!DOCTYPE html>"                                                                                                                        & vbCrLf &_
                "<html>"                                                                                                                                & vbCrLf &_
                "<head>"                                                                                                                                & vbCrLf &_
                "    <meta http-equiv='X-UA-Compatible' content='IE=edge'>"                                                                             & vbCrLf &_
                "    <meta charset='utf-8'>"                                                                                                            & vbCrLf &_
                "    <meta http-equiv='cache-control' content='no-cache' />"                                                                            & vbCrLf &_
                "    <meta http-equiv='Expires' content='-1'>"                                                                                          & vbCrLf &_
                "    <meta name='robots' content='noindex,nofollow'> "                                                                                  & vbCrLf &_
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, user-scalable=no'>"    & vbCrLf &_
                "    <title></title>"                                                                                                                   & vbCrLf &_
                "    <style> html, body {margin:0; padding:0; overflow-x:hidden; overflow-y:hidden; width:100%; height:100%;} </style> "                & vbCrLf &_
                "    <link href='https://cdn2.devy.kr/WG_SERVICE_ID/css/webgate.css?v=" & VersionTag & "' rel='stylesheet'>"                            & vbCrLf &_
                "</head>"                                                                                                                               & vbCrLf &_
                "<body>"                                                                                                                                & vbCrLf &_
                "    <script type='text/javascript' src='https://cdn2.devy.kr/WG_SERVICE_ID/js/webgate.js?v=" & VersionTag & "'></script>"              & vbCrLf &_
                "    <script>"                                                                                                                          & vbCrLf &_
                "        function WG_PageLoaded () {"                                                                                                   & vbCrLf &_
                "            WG_StartWebGate('WG_GATE_ID', window.location.href, 'BACKEND'); //reload "                                                 & vbCrLf &_
                "        }"                                                                                                                             & vbCrLf &_
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
        On Error Resume Next


        ' 1) 정규식 검증 실패 시 즉시 반환
        If Not WG_IsValidApi(Url) Then
            WG_HttpGet = ""
            Exit Function
        End If

        ' 2) HTTP 호출
        XmlHttp.open "GET", Url, False
        XmlHttp.send

        If Err.Number <> 0 Then
            WG_CallApi = ""
            Err.Clear
            Exit Function
        End If

        If XmlHttp.status = 200 Then
            WG_CallApi = XmlHttp.responseText
        Else
            WG_CallApi = ""  ' 필요하면 에러 바디 반환하도록 바꾸세요: XmlHttp.responseText
        End If

        On Error GoTo 0
    End Function



    Function WG_WriteCookie(Key,Value)
        Dim Expire : Expire = FormatDateTime(Date()+7, 2) & " " & FormatDateTime(Date()+1, 4) 
        Response.AddHeader "Set-Cookie", Key & "=" & Value & ";Path=/;Expires=" & Expire
    End Function


    Function WG_UtcTimeFromat(dt)
        WG_UtcTimeFromat = Year(dt) & "-" & Month(dt) & "-" & Day(dt) & "T" & Hour(dt) & ":" & Minute(dt) & ":" & second(dt) & "+09:00" '서울(KST)로 가정    
    End Function


    Function WG_RandomString(StrLen)
        Dim ReturnValue, i
        Const CharPool= "12345678ABCDEFGHJKLMNPQRSTWXYZ"
        Randomize
        For i = 1 to StrLen
            ReturnValue = ReturnValue & Mid(CharPool,Int((Len(CharPool)*Rnd)+1),1)
        Next
        WG_RandomString = ReturnValue
    End Function


    Function GetClientIP()
        Dim ipAddress
        ipAddress = Request.ServerVariables("HTTP_X_FORWARDED_FOR")

        If ipAddress = "" Or IsNull(ipAddress) Then
            ipAddress = Request.ServerVariables("REMOTE_ADDR")
        End If

        If IsNull(ipAddress) Then
            ipAddress = "N/A"
        End If

        GetClientIP = ipAddress
    End Function


    ' --------------------------------------------
    ' SSRF 대응을 위한 API URL 정규식 검증
    '  - 허용 패턴: ^http[s]?://\d{4}-\w{1,2}\.devy\.kr[\/\?].*$
    '  - VBScript RegExp에는 \d, \w 축약클래스가 없으므로 [0-9], [A-Za-z0-9_]로 치환
    ' --------------------------------------------
    Function WG_IsValidApi(Url)
        Dim rx : Set rx = New RegExp
        rx.Pattern    = "^https?://[0-9]{4}-[A-Za-z0-9_]{1,2}\.devy\.kr[/?].*$"
        rx.IgnoreCase = True
        rx.Global     = False

        WG_IsValidApi = rx.Test(Url)

        Set rx = Nothing
    End Function

    ' (옵션) 호스트만 있어도 허용하려면 위 패턴 대신 아래 주석을 사용하세요.
    ' rx.Pattern = "^https?://[0-9]{4}-[A-Za-z0-9_]{1,2}\.devy\.kr(?:[/?].*)?$"

%>