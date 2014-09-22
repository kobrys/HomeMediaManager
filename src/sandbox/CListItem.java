package com.hmm;

# -*- coding: utf-8 -*-
# Based on (root)/trunk/xbmc-addons/src/plugin.video.polishtv.live/hosts/ @ 419 - Wersja 605

###################################################
# LOCAL import
###################################################
from Plugins.Extensions.IPTVPlayer.components.ihost import IHost, CDisplayListItem, RetHost, CUrlItem
from Plugins.Extensions.IPTVPlayer.libs.crypto.cipher import aes_cbc, base
import Plugins.Extensions.IPTVPlayer.libs.pCommon as pCommon
from Plugins.Extensions.IPTVPlayer.tools.iptvtools import printDBG, CSelOneLink, GetLogoDir
###################################################
# FOREIGN import
###################################################
import urllib, urllib2, re, string, binascii, time, os
import xml.etree.cElementTree
from Components.config import config, ConfigSelection, ConfigYesNo, ConfigText, getConfigListEntry
try:
    from hashlib import sha1
except ImportError:
    import sha
    sha1 = sha.new
###################################################
    
def gettytul():
    return 'TVN VOD player'

###################################################
# Config options for HOST
###################################################
config.plugins.iptvplayer.TVNDefaultformat = ConfigSelection(default = "4", choices = [("0", "Najgorsza"), ("1", "Bardzo niska"), ("2", "Niska"),  ("3", "Ĺrednia"), ("4", "Standard"), ("5", "Wysoka"), ("6", "Bardzo wysoka"), ("7", "HD"), ("9999", "Najlepsza")])
config.plugins.iptvplayer.TVNUseDF = ConfigYesNo(default = False)
config.plugins.iptvplayer.TVNdevice = ConfigSelection(default = "Mobile (Android)", choices = [("Mobile (Android)", "Mobile (Android)"),("Samsung TV", "Samsung TV")])
config.plugins.iptvplayer.TVNhttpType = ConfigSelection(default = "http://", choices = [("http://", "http://"),("https://", "https://")])
config.plugins.iptvplayer.proxyenable = ConfigYesNo(default = False)
   
def GetConfigList():
    optionList = []

    optionList.append(getConfigListEntry("DomyĹlna jakoĹÄ video:", config.plugins.iptvplayer.TVNDefaultformat))
    optionList.append(getConfigListEntry("UĹźywaj domyĹlnej jakoĹci video:", config.plugins.iptvplayer.TVNUseDF))
    optionList.append(getConfigListEntry("TVN-Przedstaw siÄ jako:", config.plugins.iptvplayer.TVNdevice))
    if config.plugins.iptvplayer.TVNdevice.value == 'Samsung TV':
        optionList.append(getConfigListEntry("    Typ poĹÄczenia:", config.plugins.iptvplayer.TVNhttpType))
    optionList.append(getConfigListEntry("   TVN-korzystaj z proxy?", config.plugins.iptvplayer.proxyenable))

    return optionList
###################################################
    
qualities = [ 'Bardzo niska', 'Niska', 'Ĺrednia', 'Standard', 'Wysoka', 'Bardzo wysoka', 'HD' ]
            
def _getLinkQuality( itemLink ):
    global qualities
    
    for idx in range( len(qualities) ):
        if qualities[idx] == itemLink[0]:
            return idx + 1
    
    return 9999999

class CListItem:
    TYPE_CATEGORY = "CATEGORY"
    TYPE_VIDEO = "VIDEO"
    def __init__(self, \
                name = 'None', \
                title = 'None', \
                category = 'None', \
                urls = [], \
                id = 'None', \
                mode = '205', \
                page = '0', \
                season = '0', \
                action = 'None', \
                description = '', \
                time = '', \
                aired = '', \
                overlay = '', \
                TVShowTitle = '', \
                episode = '', \
                iconimage = '', \
                type = TYPE_CATEGORY):
        self.name = name
        self.title = title
        self.category = category
        self.urls = urls
        self.id = id
        self.mode = mode
        self.page = page
        self.season = season
        self.action = action
        self.description = description
        self.time = time
        self.aired = aired
        self.overlay = overlay
        self.TVShowTitle = TVShowTitle
        self.episode = episode  
        self.iconimage = iconimage
        self.type = type
 
# CListItem

class tvn:
    PAGE_LIMIT = 30 #ptv.getSetting('tvn_perpage')
    mode = 0
    mediaHost = 'http://redir.atmcdn.pl'
    
    mediaMainUrl = '/scale/o2/tvn/web-content/m/'
    mediaUserAgent = 'Dalvik/1.2.0 (Linux; U; Android 2.2.1; GT-I5700 Build/FRG83)'
    contentUser='atm'
    contentPass='atm_json'

    def __init__(self):
        printDBG("Starting TVN Player")
        self.currList = []
        printDBG( "TVN platform: '%s'" % config.plugins.iptvplayer.TVNdevice.value )
        if config.plugins.iptvplayer.TVNdevice.value == 'Samsung TV':
            self.contentHost = config.plugins.iptvplayer.TVNhttpType.value + 'api.tvnplayer.pl'
            self.authKey = 'ba786b315508f0920eca1c34d65534cd'
            self.startUrl = '/api/?platform=ConnectedTV&terminal=Samsung&format=xml&v=2.0&authKey=' + self.authKey
            self.contentUserAgent = 'Mozilla/5.0 (SmartHub; SMART-TV; U; Linux/SmartTV; Maple2012) AppleWebKit/534.7 (KHTML, like Gecko) SmartTV Safari/534.7'
        else:
            self.contentHost = 'http://tvnplayer.pl'
            self.authKey = 'b4bc971840de63d105b3166403aa1bea'
            self.startUrl = '/api/?platform=Mobile&terminal=Android&format=xml&v=2.0&authKey=' + self.authKey
            self.contentUserAgent = 'Apache-HttpClient/UNAVAILABLE (java 1.4)'

        proxyURL = config.plugins.iptvplayer.proxyurl.value
        useProxy = config.plugins.iptvplayer.proxyenable.value                                          
        self.cm = pCommon.common(proxyURL, useProxy)
        
    def getCurrList(self):
        return self.currList
        
    def setCurrList(self, list):
        self.currList = list
        return 
        
    def resolveUrl(self, url):
        videoUrl = ''
        if len(url) > 0:
            if config.plugins.iptvplayer.TVNdevice.value == 'Mobile (Android)':
                videoUrl = self.generateToken(url)
                printDBG('TVN - getVideoUrl() -> temporary videoUrl: ' + url)
                printDBG('TVN - getVideoUrl() -> videoUrl: ' + videoUrl)
            elif config.plugins.iptvplayer.TVNdevice.value == 'Samsung TV':
                try:
                    videoUrl = self.cm.getURLRequestData({ 'url': url, 'use_host': True, 'host': self.contentUserAgent, 'use_cookie': False, 'use_post': False, 'return_data': True })
                    printDBG('TVN - getVideoUrl() -> temporary videoUrl: ' + url)
                    printDBG('TVN - getVideoUrl() -> videoUrl: ' + videoUrl)
                except:
                    return ''
        else:
            return ''
        
        return videoUrl.encode('utf-8')
        

    def addDir(self, name, id, mode, category, iconimage, videoUrls='', listsize=0, season=0, description=''):
        
        ok = True
        item =  CListItem(name = name, \
                        title = 'None', \
                        category = category, \
                        urls = videoUrls, \
                        id = id, \
                        mode = mode, \
                        page = '0', \
                        season = str(season), \
                        description = description, \
                        iconimage = iconimage, \
                        type = CListItem.TYPE_CATEGORY)
                       
        self.currList.append(item)
    
        return ok

    def addVideoLink(self,prop,urls,iconimage,listsize=0):
        ok=True
        if not 'description' in prop:
            prop['description'] = ''
        if not 'time' in prop:
            prop['time'] = 0
        if not 'aired' in prop:
            prop['aired'] = ''
        if not 'overlay' in prop:
            prop['overlay'] = 0
        if not 'TVShowTitle' in prop:
            prop['TVShowTitle'] = ''
        if not 'episode' in prop:
            prop['episode'] = 0
        if not 'id' in prop:
            prop['id'] = '0'
        
        
        ok = True
        item =  CListItem(name = prop['title'], \
                        title = 'None', \
                        category = 'None', \
                        urls = urls, \
                        id = prop['id'], \
                        mode = '0', \
                        page = '0', \
                        season = '', \
                        description = prop['description'], \
                        time = str(prop['time']), \
                        aired = prop['aired'], \
                        overlay = prop['overlay'], \
                        TVShowTitle = prop['TVShowTitle'], \
                        episode = prop['episode'], \
                        iconimage = iconimage, \
                        type = CListItem.TYPE_VIDEO)
                       
        self.currList.append(item)
        return ok


    def listsCategories(self):

        if self.category != 'None' and self.id != 'None':
            method = 'getItems'
            groupName = 'items'
            page = 1+self.page
            urlQuery = '&type=%s&id=%s&limit=%s&page=%s&sort=newest&m=%s' % (self.category, self.id, str(tvn.PAGE_LIMIT), str(page), method)
            if self.season > 0:
                urlQuery = urlQuery + "&season=" + str(self.season)
            
        else:
            method = 'mainInfo'
            groupName = 'categories'
            urlQuery = '&m=' + method
        
        try:
            printDBG('Try TVN - listCategories() -> link: ' + self.contentHost + self.startUrl + urlQuery)
            response = self.cm.getURLRequestData({ 'url': self.contentHost + self.startUrl + urlQuery, 'use_host': True, 'host': self.contentUserAgent, 'use_cookie': False, 'use_post': False, 'return_data': True })
            xmlDoc = xml.etree.cElementTree.fromstring(response)
        except: 
            try:
                tmpUrl = string.replace(self.contentHost, 'https:', 'http:')
                printDBG('Try TVN - listCategories() -> link: ' + tmpUrl + self.startUrl + urlQuery)
                response = self.cm.getURLRequestData({ 'url': tmpUrl + self.startUrl + urlQuery, 'use_host': True, 'host': self.contentUserAgent, 'use_cookie': False, 'use_post': False, 'return_data': True })
                xmlDoc = xml.etree.cElementTree.fromstring(response)
            except:
                response = ''
                xmlDoc = xml.etree.cElementTree.fromstring(response)
        
        categories = xmlDoc.findall(method + "/" + groupName + "/row")
        countItemNode = xmlDoc.find(method + "/count_items")
        showNextPage = False
        
        try:
            countItem = int(countItemNode.text)
            if countItem > int(tvn.PAGE_LIMIT)*(1+self.page):
                showNextPage = True
        except:
            countItem = 0
        
        listsize = len(categories)
        
        seasons = xmlDoc.find(method + "/seasons")
        showSeasons = False
        # and 
        if xml.etree.cElementTree.iselement(seasons) and self.season == 0:
            seasons = seasons.findall("row")
            printDBG( "ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ: %d" % len(seasons) )
            showSeasons = True
            listsize = listsize + len(seasons)
            numSeasons = len(seasons)
        else:
            numSeasons = 0

        if self.season<>0 or (self.season==numSeasons):
            for category in categories:
                titleNode = category.find('name')
                if not xml.etree.cElementTree.iselement(titleNode):
                    titleNode = category.find('title')

                if xml.etree.cElementTree.iselement(titleNode):
                    name = titleNode.text.encode('utf-8')
                else:
                    name = 'brak'
                    
                #added by SamSamSam
                description = ''
                leadNode = category.find('lead')
                if xml.etree.cElementTree.iselement(leadNode):
                    try:
                        description = leadNode.text.encode('utf-8')
                    except:
                        description = ''
                
                episodeNode = category.find('episode')
                if xml.etree.cElementTree.iselement(episodeNode):
                    episodeNo = episodeNode.text
                    if episodeNo:
                        if episodeNode.text != "0":
                            name = name + ", odcinek " + str(episodeNode.text)
                seasonNode = category.find('season')
                if xml.etree.cElementTree.iselement(seasonNode):
                    seasonNo = seasonNode.text
                    if seasonNo:
                        if seasonNo != "0":
                            name = name + ", sezon " + str(seasonNo)

                airDateNode = category.find('start_date')
                if xml.etree.cElementTree.iselement(airDateNode):
                    airDateStr = airDateNode.text
                    if airDateStr:
                        airDate = time.strptime(airDateStr,"%Y-%m-%d %H:%M")
                        #if airDate <
                        now = time.localtime()
                        if airDate > now:
                            name = name + " (planowany)"
                            #print airDate.text

                type = category.find('type').text.encode('utf-8')
                id = category.find('id').text.encode('utf-8')
                
                # logic change by SamSamSam to speed up searching 
                # urls list will be filled when user select item
                videoUrls = []
                hasVideo = False
                if type == 'episode':
                    hasVideo = True

                iconUrl = self.getIconUrl(category)
            
            
                if not hasVideo:
                    self.addDir(name,id,self.mode,type,iconUrl,videoUrls,listsize,0,description)
                else:
                    prop = {
                        'id': id,
                        'title': name,
                        'TVShowTitle' : name,
                        'aired' : airDate,
                        'episode' : 0,
                        'description': description,
                        'time': 0
                        }
                    
                    
                    self.addVideoLink(prop,videoUrls,iconUrl,listsize)


        if showSeasons:
            for season in seasons:
                iconUrl = self.getIconUrl(season)
                self.addDir(season.find('name').text,self.id,self.mode,self.category,iconUrl,[],listsize,season.find('id').text,'')


        if showNextPage:
            self.addNextPage()

        

    def handleService(self, index, refresh = 0):
        if 0 == refresh:
            if len(self.currList) <= index:
                printDBG( "handleService wrond index: %s, len(self.currList): %d" % (index, len(self.currList)) )
                return
        
            if -1 == index:         
                self.name = 'None'
                self.title = 'None'
                self.category = 'None'
                self.urls = []
                self.id = 'None'
                self.mode = "205"
                self.page = '0'
                self.season = '0'

                printDBG( "handleService for first category" )
            else:
                item = self.currList[index]
                self.name = item.name
                self.title = item.title
                self.category = item.category
                self.urls = item.urls
                self.id = item.id
                self.mode = item.mode
                self.page = item.page
                self.season = item.season
                
                printDBG( "|||||||||||||||||||||||||||||||||||| %s " % item.name )
               
                self.title = ""
            
        self.currList = []
        
        
        if not self.page:
            self.page = 0
        else:
            self.page = int(self.page)

        printDBG("page: " + str(self.page))
        if not self.season:
            self.season = 0
        else:
            self.season = int(self.season)

        printDBG( "name:" + self.name + ", title:" + self.title + ", category:" + self.category + ", mode:" + self.mode + ", id:" + self.id + ", season:" + str(self.season) )

        self.listsCategories()   

    def getVideoUrl(self, category, id):
        method = 'getItem'
        groupName = 'item'
        urlQuery = '&type=%s&id=%s&limit=%s&page=1&sort=newest&m=%s' % (category, id, str(tvn.PAGE_LIMIT), method)
        url = self.contentHost + self.startUrl + urlQuery
        
        try:
            response = self.cm.getURLRequestData({ 'url': url , 'use_host': True, 'host': self.contentUserAgent, 'use_cookie': False, 'use_post': False, 'return_data': True })
            printDBG('TVN - getVideoUrl() -> link: ' + url)
        except:
            try:
                tmpUrl = string.replace(url, 'https:', 'http:')
                printDBG('Try TVN - getVideoUrl() -> link: ' + tmpUrl)
                response = self.cm.getURLRequestData({ 'url': tmpUrl, 'use_host': True, 'host': self.contentUserAgent, 'use_cookie': False, 'use_post': False, 'return_data': True })
            except:
                response = ''
          
        if response != 'NOTOK (permission denied)':
            xmlDoc = xml.etree.cElementTree.fromstring(response)
            runtime = xmlDoc.find(method + "/" + groupName + "/run_time")
            videoTime = 0

            if xml.etree.cElementTree.iselement(runtime):
                videoTimeStr = runtime.text
                if  videoTimeStr:
                    videoTimeElems = videoTimeStr.split(":")
                    videoTime = int(videoTimeElems[0])*60*60+int(videoTimeElems[1])*60+int(videoTimeElems[2])
            plot = xmlDoc.find(method + "/" + groupName + "/lead")
            videoPlot = ""
            if xml.etree.cElementTree.iselement(plot):
                videoPlot = plot.text
                if  videoPlot:
                    videoPlot = plot.text.encode('utf-8')

            videos = xmlDoc.findall(method + "/" + groupName + "/videos/main/video_content/row")
            videoUrls = []
            strTab = []
            for video in videos:
                qualityName = video.find('profile_name').text.encode('utf-8')
                url = video.find('url').text
                strTab.append(qualityName)
                strTab.append(url)
                videoUrls.append(strTab)
                strTab = []
            rankSorted = sorted(videoUrls)
            
            #printDBG(TVN - getVideoUrl -> video tab: ' + str(rankSorted))
            return [rankSorted, videoTime, videoPlot]
        return [[],'','']
    
    
    def generateToken(self, url):
        url = url.replace('http://redir.atmcdn.pl/http/','')
        SecretKey = 'AB9843DSAIUDHW87Y3874Q903409QEWA'
        iv = 'ab5ef983454a21bd'
        KeyStr = '0f12f35aa0c542e45926c43a39ee2a7b38ec2f26975c00a30e1292f7e137e120e5ae9d1cfe10dd682834e3754efc1733'
        salt = sha1()
        salt.update(os.urandom(16))
        salt = salt.hexdigest()[:32]

        tvncrypt = aes_cbc.AES_CBC(SecretKey, base.noPadding(), keySize=32)
        key = tvncrypt.decrypt(binascii.unhexlify(KeyStr), iv=iv)[:32]

        expire = 3600000L + long(time.time()*1000) - 946684800000L

        unencryptedToken = "name=%s&expire=%s\0" % (url, expire)

        pkcs5_pad = lambda s: s + (16 - len(s) % 16) * chr(16 - len(s) % 16)
        pkcs5_unpad = lambda s : s[0:-ord(s[-1])]

        unencryptedToken = pkcs5_pad(unencryptedToken)

        tvncrypt = aes_cbc.AES_CBC(binascii.unhexlify(key), padding=base.noPadding(), keySize=16)
        encryptedToken = tvncrypt.encrypt(unencryptedToken, iv=binascii.unhexlify(salt))
        encryptedTokenHEX = binascii.hexlify(encryptedToken).upper()

        return "http://redir.atmcdn.pl/http/%s?salt=%s&token=%s" % (url, salt, encryptedTokenHEX)

    def addNextPage(self):
        page = self.page
        if not page:
            page = 0       
        ok = True
        item =  CListItem(name = 'Nastepna Strona>', \
                        title = 'None', \
                        category = self.category, \
                        urls = self.urls, \
                        id = self.id, \
                        mode = self.mode, \
                        page = str(page+1), \
                        season = str(self.season), \
                        type = CListItem.TYPE_CATEGORY)
                       
        self.currList.append(item)
        
        return ok

    def getIconUrl(self, node):
        thumbnails = node.findall('thumbnail/row')
        iconUrl = ''
        if len(thumbnails) > 0:
            icon = thumbnails[0].find('url').text.encode('utf-8')
            printDBG('TVN - getIconUrl() -> url: ' + self.mediaHost + self.mediaMainUrl + icon + '?quality=50&dstw=290&dsth=287&type=1')
            iconUrl = self.mediaHost + self.mediaMainUrl + icon + '?quality=50&dstw=290&dsth=287&type=1'
        return iconUrl
    
    
    def getUrlFromTab(self, tab, key):
        out = ''  
        for key in qualities if key == 'Maksymalna' else [key]:
            for i in range(len(tab)):
                k = tab[i][0]
                v = tab[i][1]
                if key == k:
                    out = v
                    break
            if out != '':
                break      
        if out == '':
            tabmenu = []
            for i in range(len(tab)):
                tabmenu.append(tab[i][0])
            menu = xbmcgui.Dialog()
            item = menu.select("WybĂłr jakoĹci", tabmenu)
            #print 'item: ' + str(tabmenu[item])
            nkey = ''
            for i in range(len(tabmenu)):
                if i == item:
                    nkey = tabmenu[i]
                    ptv.setSetting('tvn_quality_temp', str(tabmenu[i]))
                    break
            for i in range(len(tab)):
                k = tab[i][0]
                v = tab[i][1]
                if nkey == k:
                    out = v
                    break
        return out

class IPTVHost(IHost):

    def __init__(self):
        self.tvn = None
        self.currIndex = -1
        self.listOfprevList = [] 
    
    # return firs available list of item category or video or link
    def getInitList(self):
        self.tvn = tvn()
        self.currIndex = -1
        self.listOfprevList = [] 
        
        self.tvn.handleService(self.currIndex)
        convList = self.convertTVPList(self.tvn.getCurrList())
        
        return RetHost(RetHost.OK, value = convList)
    
    # return List of item from current List
    # for given Index
    # 1 == refresh - force to read data from 
    #                server if possible 
    # server instead of cache 
    def getListForItem(self, Index = 0, refresh = 0, selItem = None):
        self.listOfprevList.append(self.tvn.getCurrList())
        
        self.currIndex = Index
        self.tvn.handleService(Index)
        convList = self.convertTVPList(self.tvn.getCurrList())
        
        
        return RetHost(RetHost.OK, value = convList)
        
    # return prev requested List of item 
    # for given Index
    # 1 == refresh - force to read data from 
    #                server if possible
    def getPrevList(self, refresh = 0):
        if(len(self.listOfprevList) > 0):
            tvnList = self.listOfprevList.pop()
            self.tvn.setCurrList(tvnList)
            convList = self.convertTVPList(tvnList)
            return RetHost(RetHost.OK, value = convList)
        else:
            return RetHost(RetHost.ERROR, value = [])
        
    # return current List
    # for given Index
    # 1 == refresh - force to read data from 
    #                server if possible
    def getCurrentList(self, refresh = 0):
        if refresh == 1:
            self.tvn.handleService(self.currIndex, refresh)
        convList = self.convertTVPList(self.tvn.getCurrList())
        return RetHost(RetHost.OK, value = convList)
        
    # return list of links for VIDEO with given Index
    # for given Index
    def getLinksForVideo(self, Index = 0, selItem = None):
        listLen = len(self.tvn.currList)
        if listLen < Index and listLen > 0:
            printDBG( "ERROR getLinksForVideo - current list is to short len: %d, Index: %d" % (listLen, Index) )
            return RetHost(RetHost.ERROR, value = [])
            
        if self.tvn.currList[Index].type != CListItem.TYPE_VIDEO:
            printDBG( "ERROR getLinksForVideo - current item has wrong type:  %s, type: %s requaired" % (CListItem.TYPE_VIDEO) )
            return RetHost(RetHost.ERROR, value = [])
            
        retlist = []
        videoID = self.tvn.currList[Index].id
        
        videoProp = self.tvn.getVideoUrl('episode',videoID)
        urlList = videoProp[0]
        
        if config.plugins.iptvplayer.TVNUseDF.value:
            maxRes = int(config.plugins.iptvplayer.TVNDefaultformat.value)
            urlList = CSelOneLink(urlList, _getLinkQuality, maxRes).getOneLink()
        
        for urlItem in urlList:
            nameLink = urlItem[0]
            url = urlItem[1]
            retlist.append(CUrlItem(nameLink, url, 1))
            
        return RetHost(RetHost.OK, value = retlist)
        
    # return resolved url
    # for given url
    def getResolvedURL(self, url):
        if url != None and url != '':
        
            ret = self.tvn.resolveUrl(url)
            list = []
            list.append(ret)
            
            return RetHost(RetHost.OK, value = list)
            
        else:
            return RetHost(RetHost.NOT_IMPLEMENTED, value = [])
            
    # return full path to player logo
    def getLogoPath(self):  
        return RetHost(RetHost.OK, value = [ GetLogoDir('tvnvodlogo.png') ])


    def convertTVPList(self, tvnList):
        hostList = []
        
        for tvpItem in tvnList:
            hostLinks = []
                
            type = CDisplayListItem.TYPE_UNKNOWN
            if tvpItem.type == CListItem.TYPE_CATEGORY:
                type = CDisplayListItem.TYPE_CATEGORY
            else:
                type = CDisplayListItem.TYPE_VIDEO
 
            hostItem = CDisplayListItem(name = tvpItem.name, \
                                        description = tvpItem.description, \
                                        type = type, \
                                        urlItems = hostLinks, \
                                        urlSeparateRequest = 1, \
                                        iconimage = tvpItem.iconimage)
            hostList.append(hostItem)
            
        return hostList