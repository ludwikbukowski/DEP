package hello;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

@Controller
public class UrlController {

    private Hasher hasher = new SHA1Hasher();

    @Autowired
    Environment environment;


    private String host() throws UnknownHostException {
//         return InetAddress.getLocalHost().getHostName();
        return "http://localhost";
    }

    private String fullUrl(String hash) throws UnknownHostException {
        String port = environment.getProperty("local.server.port");
        return host()+":"+port+"/short?x=" + hash;
    }

    @GetMapping("/url")
    public String greetingForm(Model model) {
        Url res = new Url();
        model.addAttribute("url", res);
        return "url";
    }

    @RequestMapping(value = "/delete", method = RequestMethod.GET)
    public ModelAndView handleDeleteUrl(@ModelAttribute Url myurl, @RequestParam(name="url")String url) throws IOException {
        Application.manager.syncRemove(url);
        myurl.setResult("");
        ModelAndView mav = new ModelAndView("result");
        try {
            mav.addObject("list",getAllUrls(Application.manager));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return mav;
    }

    @PostMapping("/url")
    public ModelAndView greetingSubmit(@ModelAttribute Url url) throws IOException {
        // use Application.manager in order to CRUD
        // Set hash here
        String urlstring = checkHttp(url.getContent());
        String hash = hasher.make(urlstring);
        String res = fullUrl(hash);
        url.setResult(res);
        Application.manager.syncPut(hash, urlstring);
        ModelAndView mav = new ModelAndView("result");
        try {
            mav.addObject("list",getAllUrls(Application.manager));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return mav;
    }

    @RequestMapping("/short")
    public RedirectView redirectPage(@RequestParam(value="x", defaultValue="nohash") String hash) throws InterruptedException {
        // Get the url based on "hash" element
        String suffix = null;
        try {
            suffix = Application.manager.syncRead(hash);
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    List<Url> getAllUrls(SyncManager manager) throws IOException, InterruptedException {
        HashMap<String, String> hashmap =  manager.dirtyList();
        List<Url> urls = new ArrayList<Url>();
        Iterator<String> keys = hashmap.keySet().iterator();
        Iterator<String> vals = hashmap.values().iterator();
        while(keys.hasNext() && vals.hasNext()){
            Url u = new Url();
            String k = keys.next();
            u.setFullcontent(fullUrl(k));
            u.setContent(k);
            u.setResult(vals.next());
            urls.add(u);
        }
        return urls;
    }

}
