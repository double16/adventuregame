Vue.component('story-create-item', {
    props: ['name', 'description', 'worldId'],
    template:  `
<div class="card">
<div class="card-body">
<h5 class="card-title">{{ name }}</h5>
<p class="card-text">{{ description }}</p>
<button v-on:click="createStory" type="button" class="btn btn-primary">Start</button>
</div>
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
<div class="card">
<div class="card-body">
    <h2 class="card-title">{{ player.fullName }}</h2>
    <h3 class="card-subtitle mb-2 text-muted">{{ player.name }}</h3>
    <p class="card-text">\${{ player.wealth }}</p>
    <div class="progress">
      health:&nbsp;<div class="progress-bar" role="progressbar" v-bind:style="{ width: (player.health/10)+'%' }" :aria-valuenow="player.health" aria-valuemin="0" aria-valuemax="1000"></div>
    </div>
    <div class="progress">
      virtue:&nbsp;<div class="progress-bar" role="progressbar" v-bind:style="{ width: (player.virtue/10)+'%' }" :aria-valuenow="player.virtue" aria-valuemin="0" aria-valuemax="1000"></div>
    </div>
    <div class="progress">
      memory:&nbsp;<div class="progress-bar" role="progressbar" v-bind:style="{ width: (player.memory/10)+'%' }" :aria-valuenow="player.memory" aria-valuemin="0" aria-valuemax="1000"></div>
    </div>
    <div class="progress">
      bravery:&nbsp;<div class="progress-bar" role="progressbar" v-bind:style="{ width: (player.bravery/10)+'%' }" :aria-valuenow="player.bravery" aria-valuemin="0" aria-valuemax="1000"></div>
    </div>
    <div class="progress">
      leadership:&nbsp;<div class="progress-bar" role="progressbar" v-bind:style="{ width: (player.leadership/10)+'%' }" :aria-valuenow="player.leadership" aria-valuemin="0" aria-valuemax="1000"></div>
    </div>
    <div class="progress">
     experience:&nbsp;<div class="progress-bar" role="progressbar" v-bind:style="{ width: (player.experience/10)+'%' }" :aria-valuenow="player.experience" aria-valuemin="0" aria-valuemax="1000"></div>
    </div>
    <div class="progress">
      agility:&nbsp;<div class="progress-bar" role="progressbar" v-bind:style="{ width: (player.agility/10)+'%' }" :aria-valuenow="player.agility" aria-valuemin="0" aria-valuemax="1000"></div>
    </div>
    <div class="progress">
      speed:&nbsp;<div class="progress-bar" role="progressbar" v-bind:style="{ width: (player.speed/10)+'%' }" :aria-valuenow="player.speed" aria-valuemin="0" aria-valuemax="1000"></div>
    </div>
</div>
</div>
`
})

Vue.component('action-request', {
    props: ['storyId', 'playerId', 'request'],
    template: `
<div><h3>{{ request.roomSummary.name }}</h3><p>{{ request.roomSummary.description }}</p><p>{{ request.roomSummary.occupants }}</p>
<form v-on:submit.prevent="performAction">
<input id="statement" size="40" v-model="statement" v-focus type="text" placeholder="What do you want to do?"/>
<button class="btn btn-primary" type="submit">Submit</button>
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
            const vue = this
            console.log("Performing action "+this.statement)
            axios.post('/api/engine/action', { storyId: this.storyId, playerId: this.playerId, statement: this.statement })
                .then(response => {
                    console.log("Action complete")
                    vue.statement = ''
                })
        },
        populateAction: function(action) {
            this.statement = action + ' '
            document.getElementById('statement').focus()
        }
    }
})

Vue.component('world-map', {
    props: [ 'map' ],
    template:
`
<svg width="300" height="300">
<g/>
</svg>
`,
    methods: {
        draw: function(el) {
            var g = new dagreD3.graphlib.Graph().setGraph({})
            if (this.map.rooms.length == 0) {
                g.setNode('?', { label: '?' })
            } else {
                this.map.rooms.forEach(room => g.setNode(room.id, { label: room.name }))
                this.map.edges.forEach(edge => g.setEdge(edge.from, edge.to, { label: edge.direction }))
            }

            // Set some general styles
            g.nodes().forEach(function(v) {
              var node = g.node(v);
              node.rx = node.ry = 5;
            })

            var svg = d3.select(el),
                inner = svg.select("g")

            // Set up zoom support
            var zoom = d3.zoom().on("zoom", function() {
                  inner.attr("transform", d3.event.transform);
                });
            svg.call(zoom)

            var render = new dagreD3.render()
            render(inner, g)

            // Center the graph
            if (g.graph().width > 0 && g.graph().height > 0) {
                var initialScale = 0.75
                svg.call(zoom.transform, d3.zoomIdentity.translate((svg.attr("width") - g.graph().width * initialScale) / 2, 20).scale(initialScale))
                svg.attr('height', g.graph().height * initialScale + 40)
            }
        }
    },
    watch: {
        map: function(newVal, oldVal) {
            this.draw(this.$el)
        }
    },
    mounted() {
        this.draw(this.$el)
    },
})

Vue.directive('focus', {
    inserted: function (el) {
        el.focus()
    }
})

const StoryCreate = {
    template: `
<div class="container">
<div class="row">
<h1>Create a New Story</h1>
</div>
    <div class="row">
        <div class="col" v-for="w in worlds">
            <story-create-item v-bind:key="w.ref" v-bind:name="w.name" v-bind:description="w.description" v-bind:worldId="w.ref"></story-create-item>
        </div>
    </div>
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
                let result = []
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
            story_id: null,
            stompClient: null,
            subscription: null,
            world: null,
            playerRequests: [],
            actionRequests: [],
            story_url: null,
            my_player_path: null,
            players: [],
            notifications: [],
            lastMap: null,
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
            for(let i = 0; i < this.players.length; i++) {
                if (this.players[i].id == this.$route.params.player_id) {
                    return this.players[i]
                }
            }
            return null
        },
        my_player_actionRequest: function() {
            for(let i = 0; i < this.actionRequests.length; i++) {
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
                for(let i = 0; i < list.length; i++) {
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
                for(let i = 0; i < list.length; i++) {
                    if (list[i].id == message.id) {
                        list.splice(i, 1)
                        return
                    }
                }
            }
        },
        waiting: function(playerId) {
            for(let i = 0; i < this.actionRequests.length; i++) {
                if (this.actionRequests[i].player.id == playerId) {
                    return true
                }
            }
            return false
        },
        distributor: function(message) {
            let disposition = message['type']
            let type, id, body
            if (disposition === 'ErrorMessage') {
                type = 'ErrorMessage'
                id = message.httpCode
                body = message
            } else if (message['request']) {
                body = message.request
                type = body['@class']
                id = body['id']
            } else if (disposition === 'PlayerChanged') {
                body = message.player
                type = disposition
                id = body['id']
                if (body.motivator != 'HUMAN') {
                    return
                }
            } else if (disposition === 'PlayerNotification') {
                body = message
                type = disposition
                id = body.player.id
            } else if (disposition === 'StoryEnded') {
                let endUrl = this.my_player_path.replace('play', 'end')
                console.log('Story ended: '+endUrl)
                this.$router.push(endUrl)
                return
            } else if (disposition === 'ChronosChanged') {
                if (message['current'] == 1) {
                    if (this.$route.name != 'run') {
                        if (this.my_player_path) {
                            this.$router.push(this.my_player_path)
                        } else {
                            this.$router.push('/notplaying')
                        }
                    }
                } else {
                    this.notifications.splice(0)
                }
                return
            } else if (disposition === 'GoalFulfilled') {
                type = disposition
                body = message.goal
                id = body.id
            } else if (disposition === 'MapMessage') {
                if (message.player.id == this.$route.params.player_id) {
                    type = disposition
                    body = message
                    id = body.player.id
                }
            } else {
                console.log('unknown message: '+message)
                return
            }
            console.log('dist: '+type+' '+id)

            switch (type) {
                case 'ErrorMessage':
                    // TODO: better reporting to the user
                    console.log(body.httpCode+': '+body.message)
                    if (id == 404) {
                        this.$router.push({ name: 'create' })
                    } else {
                        this.addToListById(this.notifications, {
                            id: id,
                            subject: 'error',
                            text: body.message
                        })
                    }
                    break
                case '.PlayerRequest':
                    if (disposition === 'RequestCreated') {
                        this.addToListById(this.playerRequests, body)
                    } else if (disposition === 'RequestSatisfied') {
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
                                    this.addToListById(this.notifications, {
                                        id: id,
                                        subject: body.player.fullName,
                                        text: body.player.fullName+': '+message.action.text
                                    })
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
                    let notification = {
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
                case 'GoalFulfilled':
                    this.addToListById(this.notifications, {
                        id: id,
                        subject: 'Goal met',
                        text: body.description
                    })
                    break
                case 'MapMessage':
                    this.lastMap = body
                    break
            }
        },
        reset: function() {
            if (this.subscription) {
                this.subscription.unsubscribe()
                this.subscription = null
            }
            this.story_id = null
            this.world = null
            this.playerRequests.splice(0)
            this.actionRequests.splice(0)
            this.story_url = null
            this.my_player_path = null
            this.players.splice(0)
            this.notifications.splice(0)
        },
        init: function() {
            const storyId = this.story_id
            const vue = this

            axios.get('/api/stories/'+storyId+'/world')
                .then(response => {
                    vue.world = {
                        name: response.data.name,
                        description: response.data.description,
                        author: response.data.author,
                    }
                })
        },
        subscribe: function() {
            const storyId = this.story_id
            const vue = this
            if (vue.subscription) {
                return
            }
            vue.subscription = vue.stompClient.subscribe("/topic/story."+storyId, function(message) {
                console.log('MSG: '+message.body)
                let json = JSON.parse(message.body)
                if (Array.isArray(json)) {
                    json.forEach(el => vue.distributor(el))
                } else {
                    vue.distributor(json)
                }
            })
        },
    },
    beforeRouteUpdate (to, from, next) {
        this.reset()
        this.story_id = to.params.story_id
        this.init()
        // assumption: stomp is connected
        this.subscribe()
        next()
    },
    mounted() {
        this.story_id = this.$route.params.story_id
        const vue = this
        vue.stompClient = new StompJs.Client({
          brokerURL: "ws://"+window.location.host+"/socket",
          debug: function (str) {
            console.log(str);
          }
        })

        vue.stompClient.onStompError = function (frame) {
          console.log('Broker reported error: ' + frame.headers['message'])
          console.log('Additional details: ' + frame.body)
        }

        vue.stompClient.onConnect =  function(frame) {
            vue.subscribe()
        }

        vue.stompClient.activate()
        vue.init()
    },
    destroyed() {
        this.reset()
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
  <player-detail v-if="request && request.template" v-bind:player="request.template"></player-detail>
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
            this.request = request
            this.fullName = request.template.fullName
            this.nickName = request.template.nickName
        },
        createPlayer: function() {
            console.log("Creating player "+this.fullName)
            axios.post('/api/engine/addtocast',
                {
                    storyId: this.$parent.story_id,
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
            axios.post('/api/engine/start', { storyId: this.$parent.story_id } )
                .then(response => {
                    console.log('Starting story, redirecting player to: '+this.$parent.my_player_path)
                    if (vue.$parent.$route.name !== 'run') {
                        vue.$parent.$router.push(this.$parent.my_player_path)
                    }
                })
        },
        reset: function() {
            this.players.splice(0)
            this.request = null
            this.fullName = null
            this.nickName = null
        },
        init: function() {
            this.$parent.story_url = window.location
        },
    },
    data() {
        return {
            players: [],
            request: null,
            fullName: null,
            nickName: null,
        }
    },
    beforeRouteUpdate(to, from, next) {
        this.reset()
        this.init()
        next()
    },
    mounted() {
        this.init()
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
    <div class="col-12">
    <world-map v-if="this.$parent.lastMap" v-bind:map="this.$parent.lastMap"></world-map>
    </div>
</div>
<div class="row">
<div class="col-6">
    <div v-if="this.$parent.my_player_obj"><h2>{{ this.$parent.my_player_obj.fullName }}</h2></div>
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
              health:&nbsp;<div class="progress-bar" role="progressbar" v-bind:style="{ width: (r.health/10)+'%' }" :aria-valuenow="r.health" aria-valuemin="0" aria-valuemax="1000"></div>
            </div>
        </div>
    </div>
</div>
</div>
</div>
`,
    methods: {
        init: function(route) {
            if (!this.$parent.my_player_path) {
                this.$parent.my_player_path = '/play/'+route.params.story_id+'/'+route.params.player_id
            }
        },
    },
    beforeRouteUpdate (to, from, next) {
        this.init(to)
        next()
    },
    mounted() {
        this.init(this.$route)
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
    <div class="row" v-if="name">
        <h1>{{ name }}</h1>
    </div>
    <div class="row" v-if="description">
        <p>{{ description }}</p>
    </div>
    <div class="row" v-if="goals.length > 0">
        <h3>Goals met:</h3>
        <ul>
            <li v-for="g in goals">{{ g }}</li>
        </ul>
    </div>
</div>
`,
    data() {
        return {
            name: null,
            description: null,
            author: null,
            goals: [],
        }
    },
    methods: {
        populate: function(route) {
            const storyId = route.params.story_id
            const vue = this

            axios.get('/api/stories/'+storyId+'/world')
                .then(response => {
                    vue.name = response.data.name
                    vue.description = response.data.description
                    vue.author = response.data.author
                })
            axios.get('/api/stories/'+storyId)
                .then(response => {
                    for(let i = 0; i < response.data.goals.length; i++) {
                        if (response.data.goals[i].fulfilled) {
                            vue.goals.push(response.data.goals[i].goal.description)
                        }
                    }
                })
        }
    },
    beforeRouteUpdate(to, from, next) {
        this.populate(to)
        next()
    },
    mounted() {
        this.populate(this.$route)
    }
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
