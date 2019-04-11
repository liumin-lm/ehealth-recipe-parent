package recipe.drugsenterprise.bean;

import java.io.Serializable;

public class TokenData implements Serializable {
        private static final long serialVersionUID = 7279334014259634268L;
        private String app_key;

        private String access_token;

        private String expires_time;

        public String getApp_key() {
            return app_key;
        }

        public void setApp_key(String app_key) {
            this.app_key = app_key;
        }

        public String getAccess_token() {
            return access_token;
        }

        public void setAccess_token(String access_token) {
            this.access_token = access_token;
        }

        public String getExpires_time() {
            return expires_time;
        }

        public void setExpires_time(String expires_time) {
            this.expires_time = expires_time;
        }

       @Override
       public String toString() {
           return "TokenData{" +
                   "app_key='" + app_key + '\'' +
                   ", access_token='" + access_token + '\'' +
                   ", expires_time='" + expires_time + '\'' +
                   '}';
       }
   }