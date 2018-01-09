package hello;

import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Controller
public class GreetingController {

    private Hasher hasher = new SHA1Hasher();

    @Autowired
    Environment environment;


    private String host() throws UnknownHostException {
//         return InetAddress.getLocalHost().getHostName();
        return "http://localhost";
    }

    @GetMapping("/greeting")
    public String greetingForm(Model model) {
        Greeting res = new Greeting();
        model.addAttribute("greeting", res);
        return "greeting";
    }

    @PostMapping("/greeting")
    public String greetingSubmit(@ModelAttribute Greeting greeting) throws IOException {
        // use Application.manager in order to CRUD
        // Set hash here
        String url = checkHttp(greeting.getContent());
        String hash = hasher.make(url);
        String port = environment.getProperty("local.server.port");
        String prefix = host()+":"+port+"/url?x=" + hash;
        greeting.setResult(prefix);
        Application.manager.syncPut(hash, url);
        return "result";
    }

    @RequestMapping("/url")
    public RedirectView redirectPage(@RequestParam(value="x", defaultValue="nohash") String hash) {
        // Get the url based on "hash" element
        String suffix = Application.manager.dirtyRead(hash);
        RedirectView redirectView = new RedirectView();
        redirectView.setUrl(suffix);
        return redirectView;
    }

    private String checkHttp(String url){
        if(url.startsWith("http://")){
            return url;
        }else if(url.startsWith("https://")){
            return url;
        }else{
            return "http://" + url;
        }
    }

}
