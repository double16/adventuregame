Vue.component('story-create-item', {
    props: ['name', 'description', 'worldId'],
    template:  `
<div><h2>{{ name }}</h2><p>{{ description }}</p>
<button v-on:click="createStory" type="submit" class="btn btn-primary">Start</button>
</div>
`,
    methods: {
        createStory: function() {
            console.log("Creating story "+this.worldId)
            axios.post('/api/engine/createstory', { worldId: this.worldId })
                .then(response => {
                    console.log("New story link: "+response.data.storyUri)
                    this.$router.push(response.data.storyUri)
                })
        }
    }
})

Vue.component('player-detail', {
    props: ['player'],
    template: `
<div>
<h2>{{ player.fullName }}</h2>
</div>
`
})

Vue.component('action-request', {
    props: ['storyId', 'playerId', 'request'],
    template: `
<div><h3>{{ request.roomSummary.name }}</h3><p>{{ request.roomSummary.description }}</p><p>{{ request.roomSummary.occupants }}</p>
<form v-on:submit.prevent="performAction">
<input id="statement" size="40" v-model="statement" v-focus type="text" placeholder="What do you want to do?"/>
<button class="btn btn-light" type="button" data-toggle="collapse" :data-target="\'#actions\'+request.id" v-if="request.actions"><span class="fas fa-question-circle"></span></button>
<p class="collapse" :id="\'actions\'+request.id">
<button type="button" v-for="action in request.actions" class="btn btn-outline-secondary btn-sm" v-on:click="populateAction(action)">{{ action }}</button>
</p>
</form>
</div>
`,
    data() {
        return {
            statement: "",
        }
    },
    methods: {
        performAction: function() {
            console.log("Performing action "+this.statement)
            axios.post('/api/engine/action', { storyId: this.storyId, playerId: this.playerId, statement: this.statement })
                .then(response => {
                    console.log("Action complete")
                })
        },
        populateAction: function(action) {
            this.statement = action + ' '
            document.getElementById('statement').focus()
        }
    }
})

Vue.directive('focus', {
    inserted: function (el) {
        el.focus()
    }
})

const StoryCreate = {
    template: `
<div class="container">
<h1>Create a New Story</h1>
<story-create-item v-for="w in worlds" v-bind:key="w.ref" v-bind:name="w.name" v-bind:description="w.description" v-bind:worldId="w.ref"></story-create-item>
</div>
`,
    data() {
        return {
            worlds: []
        }
    },
    mounted() {
        console.log('loading world list')
        axios.get('/api/worlds')
              .then(response => {
                var result = []
                response.data._embedded.worlds.forEach(world => {
                    result.push({
                      name: world.name,
                      description: world.description,
                      ref: world._links.self.href
                    })
                })
                this.worlds = result
              })
    }
}

const Story = {
    template: `
<div>
<div class="container">
<div class="row" v-if="world">
<h1>{{ world.name }}</h1>
<button class="btn btn-light" type="button" data-toggle="collapse" data-target="#story_description" v-if="world.description"><span class="fas fa-question-circle"></span></button>
</div>
<div class="row" v-if="world">
<p class="collapse" id="story_description">{{ world.description }}</p>
</div>
</div>
<router-view></router-view>
</div>
`,
    data() {
        return {
            stompClient: null,
            subscription: null,
            world: null,
            playerRequests: [],
            actionRequests: [],
            story_url: null,
            my_player_path: null,
            players: [],
            notifications: []
        }
    },
    computed: {
        my_player_url: function() {
            if (!this.my_player_path) {
                return null
            }
            return new URL("#"+this.my_player_path, window.location).toString()
        },
        my_player_obj: function() {
            for(var i = 0; i < this.players.length; i++) {
                if (this.players[i].id == this.$route.params.player_id) {
                    return this.players[i]
                }
            }
            return null
        },
        my_player_actionRequest: function() {
            for(var i = 0; i < this.actionRequests.length; i++) {
                if (this.actionRequests[i].player.id == this.$route.params.player_id) {
                    return this.actionRequests[i]
                }
            }
            return null
        }
    },
    methods: {
        addToListById: function(list, message) {
            if (message.id) {
                for(var i = 0; i < list.length; i++) {
                    if (list[i].id == message.id) {
                        list[i] = message
                        return
                    }
                }
            }
            list.push(message)
        },
        removeFromListById: function(list, message) {
            if (message.id) {
                for(var i = 0; i < list.length; i++) {
                    if (list[i].id == message.id) {
                        list.splice(i, 1)
                        return
                    }
                }
            }
        },
        waiting: function(playerId) {
            for(var i = 0; i < this.actionRequests.length; i++) {
                if (this.actionRequests[i].player.id == playerId) {
                    return true
                }
            }
            return false
        },
        distributor: function(message) {
            var disposition = message['type']
            var type, id, body
            if (disposition == 'ErrorMessage') {
                // TODO: better reporting to the user
                console.log(message.httpCode+': '+message.message)
                this.$router.push({ name: 'create' })
                return
            } else if (message['request']) {
                body = message.request
                type = body['@class']
                id = body['id']
            } else if (disposition == 'PlayerChanged') {
                body = message.player
                type = disposition
                id = body['id']
                if (body.motivator != 'HUMAN') {
                    return
                }
            } else if (disposition == 'PlayerNotification') {
                body = message
                type = disposition
                id = body.player.id
            } else if (disposition == 'StoryEnded') {
                var endUrl = this.my_player_path.replace('play', 'end')
                console.log('Story ended: '+endUrl)
                this.$router.push(endUrl)
                return
            } else if (disposition == 'ChronosChanged') {
                if (message['current'] == 1) {
                    if (this.$route.name != 'run') {
                        if (this.my_player_path) {
                            this.$router.push(this.my_player_path)
                        } else {
                            this.$router.push('/notplaying')
                        }
                    }
                } else {
                    this.notifications.splice(0, this.notifications.length)
                }
            } else {
                console.log('unknown message: '+message)
                return
            }
            console.log('dist: '+type+' '+id)

            switch (type) {
              case '.PlayerRequest':
                if (disposition == 'RequestCreated') {
                    this.addToListById(this.playerRequests, body)
                } else if (disposition == 'RequestSatisfied') {
                    this.removeFromListById(this.playerRequests, body)
                }
                break
              case '.ActionRequest':
                if (disposition == 'RequestCreated') {
                    this.addToListById(this.actionRequests, body)
                } else if (disposition == 'RequestSatisfied') {
                    this.removeFromListById(this.actionRequests, body)
                    if (body.player.id == this.$route.params.player_id) {
                        this.removeFromListById(this.notifications, { id: body.player.id })
                    } else {
                        if (message.action && message.action.text) {
                                var notification = {
                                    id: id,
                                    subject: body.player.fullName,
                                    text: body.player.fullName+': '+message.action.text
                                }
                                this.addToListById(this.notifications, notification)
                           }
                    }
                }
                break
              case 'PlayerChanged':
                this.addToListById(this.players, body)
                break
              case 'PlayerNotification':
                if (body.player.id != this.$route.params.player_id) {
                    break
                }
                var notification = {
                    id: body.player.id,
                    subject: message.subject,
                    text: message.text
                }
                if (notification.text) {
                    this.addToListById(this.notifications, notification)
                } else {
                    this.removeFromListById(this.notifications, notification)
                }
                break
            }
        }
    },
    beforeRouteUpdate (to, from, next) {
        // TODO: react to route changes...
        next()
    },
    mounted() {
        const storyId = this.$route.params.story_id
        var vue = this

        axios.get('/api/stories/'+storyId+'/world')
            .then(response => {
                vue.world = {
                  name: response.data.name,
                  description: response.data.description,
                  author: response.data.author,
                }
            })

        var stompClient = vue.stompClient
        stompClient = new StompJs.Client({
          brokerURL: "ws://"+window.location.host+"/socket",
          debug: function (str) {
            console.log(str);
          }
        })

        stompClient.onConnect =  function(frame) {
            vue.subscription = stompClient.subscribe("/topic/story."+storyId, function(message) {
                console.log('MSG: '+message.body)
                var json = JSON.parse(message.body)
                if (Array.isArray(json)) {
                    json.forEach(el => vue.distributor(el))
                } else {
                    vue.distributor(json)
                }
            })
        }

        stompClient.onStompError = function (frame) {
          console.log('Broker reported error: ' + frame.headers['message'])
          console.log('Additional details: ' + frame.body)
        }

        stompClient.activate()
    },
    destroyed() {
        if (this.subscription) {
            this.subscription.unsubscribe()
        }
        if (this.stompClient) {
            this.stompClient.deactivate()
        }
    }
}

const StoryInit = {
    template: `
<div class="container">
<div class="row">Invite others with this link: {{ this.$parent.story_url }}</div>
<div class="row" v-if="this.$parent.my_player_url">Continue with this link (or bookmark this page): {{ this.$parent.my_player_url }}</div>
<div class="row">
<button type="button" class="btn btn-sm btn-primary" v-on:click="startStory" v-if="this.$parent.my_player_url">Start Story</button>
</div>
<div class="row" v-if="!$parent.my_player_path">
<div class="col">
<div class="card" v-for="r in this.$parent.playerRequests" :key="r.id">
    <div class="card-body">
        <h5 class="card-title">{{ r.template | templateDisplayName }}</h5>
        <button type="button" class="btn btn-sm btn-primary" v-on:click="selectPlayer(r)">Select</button>
    </div></div>
</div>
<div class="col">
<form>
  <div class="form-group">
    <label for="fullName">Full Name</label>
    <input type="text" class="form-control" v-model="fullName" placeholder="Enter player full name">
  </div>
  <div class="form-group">
    <label for="nickName">Nick Name</label>
    <input type="text" class="form-control" v-model="nickName" placeholder="Enter player nick name">
  </div>
  <button type="button" class="btn btn-primary" v-on:click="createPlayer">Submit</button>
  <div class="alert alert-warning"><p>TODO: full info about character</p></div>
</form>
</div>
</div>
</div>
`,
    filters: {
        templateDisplayName: function(template) {
            if (!template.fullName) {
                return template.name
            }
            return template.fullName + "(" + template.name + ")"
        }
    },
    methods: {
        selectPlayer: function(request) {
            this.request = request,
            this.fullName = request.template.fullName
            this.nickName = request.template.nickName
        },
        createPlayer: function() {
            console.log("Creating player "+this.fullName)
            axios.post('/api/engine/addtocast',
                {
                    storyId: this.$parent.$route.params.story_id,
                    playerTemplateId: this.request.template.id,
                    motivator: 'HUMAN',
                    fullName: this.fullName,
                    nickName: this.nickName
                }
            )
                .then(response => {
                    console.log("New player link: "+response.data.playerUri)
                    this.$parent.my_player_path = response.data.playerUri
                })
        },
        startStory: function() {
            const vue = this
            axios.post('/api/engine/start', { storyId: this.$parent.$route.params.story_id } )
                .then(response => {
                    console.log('Starting story, redirecting player to: '+this.$parent.my_player_path)
                    if (vue.$parent.$route.name != 'run') {
                        vue.$parent.$router.push(this.$parent.my_player_path)
                    }
                })
        }
    },
    data() {
        return {
            players: [],
            request: null,
            fullName: null,
            nickName: null,
        }
    },
    mounted() {
        this.$parent.story_url = window.location
    }
}

const StoryRun = {
    template: `
<div class="container">
<div class="row">Continue with this link (or bookmark this page): {{ this.$parent.my_player_url }}</div>
<div class="row">
    <div class="alert alert-warning alert-dismissible fade show" role="alert" v-for="n in this.$parent.notifications" :key="n.id">
        {{ n.text }}
        <button type="button" class="close" data-dismiss="alert" aria-label="Close"><span aria-hidden="true">&times;</span></button>
    </div>
</div>
<div class="row">
<div class="col-8">
    <player-detail v-if="this.$parent.my_player_obj" v-bind:player="this.$parent.my_player_obj"></player-detail>
    <action-request v-if="this.$parent.my_player_actionRequest"
      v-bind:storyId="$parent.$route.params.story_id"
      v-bind:playerId="$parent.$route.params.player_id"
      v-bind:request="this.$parent.my_player_actionRequest"></action-request>
    <i class="fas fa-clock fa-9x" v-else></i>
</div>
<div class="col-4">
    <h3>Players</h3>
    <div class="card" v-for="r in this.$parent.players" :key="r.id">
        <div class="card-body">
            <h5 class="card-title">{{ r.nickName }}&nbsp;<i class="fas fa-clock" title="Waiting for player to move" v-if="$parent.waiting(r.id)"></i></h5>
            <h6 class="card-subtitle mb-2 text-muted">{{ r.room.name }}</h6>
            <p class="card-text">\${{ r.wealth }}</p>
            <div class="progress">
              <div class="progress-bar" role="progressbar" v-bind:style="{ width: (r.health/10)+'%' }" :aria-valuenow="r.health" aria-valuemin="0" aria-valuemax="1000">health</div>
            </div>
        </div>
    </div>
</div>
</div>
</div>
`,
    beforeRouteUpdate (to, from, next) {
        // TODO: react to route changes...
        next()
    },
    mounted() {
        if (!this.$parent.my_player_path) {
            this.$parent.my_player_path = '/play/'+this.$route.params.story_id+'/'+this.$route.params.player_id
        }
    }
}

const StoryEnd = {
    template: `
<div class="container">
    <div class="row">
        <div class="alert alert-success">
            Story has ended.
        </div>
    </div>
</div>
`
}

const StoryNotPlaying = {
    template: `
<div class="container">
    <div class="row">
        <div class="alert alert-danger">
            Story started without you <i class="fas fa-sad-tear"></i>
        </div>
    </div>
</div>
`
}

const router = new VueRouter({
  routes: [
     { path: '/play/:story_id', component: Story,
       children: [
            { path: '', name: 'init', component: StoryInit },
            { path: '/play/:story_id/:player_id', name: 'run', component: StoryRun },
       ]
     },
     { path: '/end/:story_id/:player_id', name: 'end', component: StoryEnd },
     { path: '/notplaying', name: 'notplaying', component: StoryNotPlaying },
     { path: '/', name: 'create', component: StoryCreate }
 ],

})

const app = new Vue({
  router
}).$mount('#app')
