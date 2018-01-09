package hello;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.net.UnknownHostException;

@Controller
public class UrlController {

    private Hasher hasher = new SHA1Hasher();

    @Autowired
    Environment environment;


    private String host() throws UnknownHostException {
//         return InetAddress.getLocalHost().getHostName();
        return "http://localhost";
    }

    @GetMapping("/url")
    public String greetingForm(Model model) {
        Url res = new Url();
        model.addAttribute("url", res);
        return "url";
    }

    @PostMapping("/url")
    public String greetingSubmit(@ModelAttribute Url greeting) throws IOException {
        // use Application.manager in order to CRUD
        // Set hash here
        String url = checkHttp(greeting.getContent());
        String hash = hasher.make(url);
        String port = environment.getProperty("local.server.port");
        String prefix = host()+":"+port+"/short?x=" + hash;
        greeting.setResult(prefix);
        Application.manager.syncPut(hash, url);
        return "result";
    }

    @RequestMapping("/short")
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
