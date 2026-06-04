/*
* ==============================================================================================
* 메가펜스 유량제어서비스 SAMPLE (ASP.NET Core 10) - 호스팅 진입점
* ----------------------------------------------------------------------------------------------
* MVC(Controllers + Views) 구성. 대문(Home/Index) 액션에서 [WebGateFilter]로 유량제어 적용.
* ==============================================================================================
*/

var builder = WebApplication.CreateBuilder(args);

// MVC(Controllers + Views) 서비스 등록
builder.Services.AddControllersWithViews();

var app = builder.Build();

if (!app.Environment.IsDevelopment())
{
    app.UseExceptionHandler("/Home/Error");
}

app.UseRouting();

// 기본 라우트 : 대문 = Home/Index
app.MapControllerRoute(
    name: "default",
    pattern: "{controller=Home}/{action=Index}/{id?}");

app.Run();
