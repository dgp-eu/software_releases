function openNav() {
    document.getElementById("mySidebar").style.width = "15rem";
    document.getElementById("TitleContainer").style.marginLeft = "15rem";
    document.getElementById("main").style.marginLeft = "15rem";
    document.getElementById("footer").style.marginLeft = "15rem";
    document.getElementById("openBtn").style.color = "rgba(0,0,0,0);";
}

function closeNav() {
    document.getElementById("mySidebar").style.width = "0";
    document.getElementById("TitleContainer").style.marginLeft= "0";
    document.getElementById("main").style.marginLeft= "0";
    document.getElementById("footer").style.marginLeft = "0";
    document.getElementById("openBtn").style.color = "rgba(0,0,0,1);";
}