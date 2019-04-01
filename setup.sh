sudo apt-get install supervisor
sudo cp supervisord.conf /etc/supervisor/supervisord.conf
sudo apt-get install avahi-daemon
sudo cp ifit.service /etc/avahi/services/ifit.service
sudo systemctl enable avahi-daemon.service
sudo supervisorctl update