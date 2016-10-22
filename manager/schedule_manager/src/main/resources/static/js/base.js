/**
 * Created by zhaoxin on 2015/8/13.
 */
$(function(){
    //导航左右滑动的显示隐藏
    function slide(){
        var maxLength = $(".m_rightHeadUl").width();
        if(maxLength > $(".m_leftNavS").width()){
            $(".m_rightSlide").show();
        }else{
            $(".m_rightSlide").hide();
            $(".m_moveDiv").css({
                left:0
            });
        }
    }


//    内容区自适应
    function adapt(){
        $(".m_leftNavS").width($(".m_rightHead").width()-60);
        $(".m_iframeStyle").height($(window).height() - $(".main-header>.navbar").height() - $(".m_rightHead").height() - $("footer").height() - 89);
        slide();
    }
    adapt();
    $(window).resize(function(){
        adapt();
    });

    //点击左边切换
    var x = parseInt(199);
    $(".treeview-menu li").on("click",function(){
        var flag = true;
        var src = $(this).attr("data");
        $(".treeview-menu li").removeClass("active");
        $(this).addClass("active");
        var text = $(this).text();
       $(".m_open").each(function(){
           if($(this).text() == text){
               flag = false;
                $(this).trigger("click");
           }
       });
        if(flag){
            x++;
            var className = "js_wrap"+x;
            $(".m_rightHeadUl").append(' <li><span class="m_open m_curr" data='+className+'>'+text+'</span><i class="m_close"></i></li>');

            $(".m_wrap").append('<div class="m_iframeWrap '+className+'"><iframe src='+src+' frameborder="0" class="m_iframeStyle" scrolling="auto"></iframe></div>');
            adapt();

            $(".m_curr").trigger("click");
        }

    });
    //关闭顶部选项卡
    $("body").on("click",".m_close",function(){
        var text = $(this).parents("li").find("span").attr("data");
        $('.m_iframeWrap').each(function(){
            if($(this).hasClass(text) ) {
                $(this).remove();
            }
        });
        $(this).parents("li").remove();
        slide();
    });
    $("body").on("click",".m_open",function(){
       $(".m_rightHeadUl>li").removeClass("m_addClass");
       $(this).parents("li").addClass("m_addClass");
        var  Class = $(this).attr("data");
        $(".m_iframeWrap").hide();
        $(".m_iframeWrap").each(function(){
           if($(this).hasClass(Class)) {
               $(this).fadeIn(200);
           }
        });
        var text = $(this).text();
        $(".treeview-menu>li").each(function(){
           if($(this).text() == text){
               $(".treeview-menu>li").removeClass("active");
               $(this).addClass("active");
           }
        });

    });

    var i = 1;
    $(".m_rightNavSlide").on("click",function(){
        var maxLength = $(".m_rightHeadUl").width();
        if(200*(i+1) < maxLength && maxLength > $(".m_leftNavS").width()){
            $(".m_moveDiv").css({
                left: - 200*i
            });
            i++;
        }

    });
    $(".m_leftNavSlide").on("click",function(){
        if(i>1){
            $(".m_moveDiv").css({
                left: - 200*(i-2)
            });
            i--;
        }
    })
});