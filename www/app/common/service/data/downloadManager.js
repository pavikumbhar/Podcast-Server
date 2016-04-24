import {Module, Service} from '../../../decorators';
import AngularStompDKConfig from '../../../config/ngstomp';
import Rx from 'rx-dom';

@Module({
    name : 'ps.common.service.data.downloadManager',
    modules : [ AngularStompDKConfig ]
})
@Service('DonwloadManager')
export default class DownloadManager {

    WS_DOWNLOAD_BASE = '/app/download';
    SSE_DOWNLOADING = '/api/task/downloadManager/downloading';

    constructor(ngstomp, $http) {
        "ngInject";
        this.$http = $http;
        this.ngstomp = ngstomp;


        this.downloading$ = Rx.DOM.fromEventSource(this.SSE_DOWNLOADING).map(v => JSON.parse(v));
    }

    download(item) {
        return this.$http.get(`/api/item/${item.id}/addtoqueue`);
    }
    stopAllDownload () {
        return this.$http.get(`/api/task/downloadManager/stopAllDownload`);
    }
    pauseAllDownload () {
        return this.$http.get(`/api/task/downloadManager/pauseAllDownload`);
    }
    restartAllDownload() {
        return this.$http.get(`/api/task/downloadManager/restartAllDownload`);
    }
    removeFromQueue (item) {
        return this.$http.delete(`/api/task/downloadManager/queue/${item.id}`);
    }
    updateNumberOfSimDl(number) {
        return this.$http.post(`/api/task/downloadManager/limit`, number);
    }
    dontDonwload (item) {
        return this.$http.delete(`/api/task/downloadManager/queue/${item.id}/andstop`);
    }
    getNumberOfSimDl() {
        return this.$http.get(`/api/task/downloadManager/limit`).then(r => r.data);
    }
    moveInWaitingList(item, position) {
        return this.$http.post(`/api/task/downloadManager/move`, {id : item.id, position });
    }
    toggle(item) {
        this.ngstomp.send(`${this.WS_DOWNLOAD_BASE}/toogle`, item);
    }
    stop(item) {
        this.ngstomp.send(`${this.WS_DOWNLOAD_BASE}/stop`, item);
    }
}