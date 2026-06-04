<%@ Page Language="C#" AutoEventWireup="true" CodeBehind="Default.aspx.cs" Inherits="AspNetWebForm.Default" %>

<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">
<head runat="server">
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>메가펜스 WebForm 데모 - 대문</title>
</head>
<body>
    <form id="form1" runat="server">
        <div id="app" class="container">
            <h1>SAMPLE BACKEND PAGE (ASP.NET WebForm)</h1>
            <p>Backend 방식의 유량제어가 적용된 대문(업무) 페이지입니다.</p>
            <p>페이지 로드 시 <code>Page_Load</code>에서 메가펜스 유량제어 호출코드가 동작합니다. (Replace 방식)</p>
        </div>
    </form>
</body>
</html>
