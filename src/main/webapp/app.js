Vue.component('story-create-item', {
    props: ['name', 'description', 'worldId'],
    template:  '<div><h2>{{ name }}</h2><p>{{ description }}</p>' +
             '<button v-on:click="createStory" type="submit" class="btn btn-primary">Start</button>' +
             '</div>',
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

const StoryCreate = {
    template: '<div class="container">' +
        '<h1>Create a New Story</h1>' +
        '<story-create-item v-for="w in worlds" v-bind:key="w.ref" v-bind:name="w.name" v-bind:description="w.description" v-bind:worldId="w.ref"></story-create-item>' +
        '</div>',
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
<div class="row">Story {{ $route.params.story_id }}</div>
</div>
<router-view></router-view>
</div>
`,
    data() {
        return {
            stompClient: null,
            subscription: null,
            playerRequests: [],
            actionRequests: [],
            my_player_url: null,
            players: []
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
        distributor: function(message) {
            var type, id, body
            if (message['request']) {
                body = message.request
                type = body['@class']
                id = body['id']
            } else if (message['player']) {
                body = message.player
                type = 'player'
                id = body['id']
            } else {
                console.log('unknown message: '+message)
                return
            }
            console.log('dist: '+type+' '+id)

            switch (type) {
              case '.PlayerRequest':
                this.addToListById(this.playerRequests, body)
                break
              case '.ActionRequest':
                this.addToListById(this.actionRequests, body)
                break
              case 'player':
                this.addToListById(this.players, body)
                break
            }
        }
    },
    mounted() {
        const storyId = this.$route.params.story_id
        var vue = this
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
<div class="row">Invite others with this link: ${window.location}</div>
<div class="row">
<button type="button" class="btn btn-sm btn-primary" v-on:click="startStory">Start Story</button>
</div>
<div class="row" v-if="!$parent.my_player_url">
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
  <button type="submit" class="btn btn-primary" v-on:click="createPlayer">Submit</button>
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
                    this.$parent.my_player_url = response.data.playerUri
                })
        },
        startStory: function() {
            const vue = this
            axios.post('/api/engine/start', { storyId: this.$parent.$route.params.story_id } )
                .then(response => {
                    vue.$parent.$router.push(this.$parent.my_player_url)
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
    }
}

const StoryRun = {
    template: `
<div class="container">
<div class="row">Continue with this link: ${window.location}</div>
<div class="row">
<div class="col">
    <ul class="list-group" v-for="r in this.$parent.players" :key="r.id">
        <li class="list-group-item">{{ r.fullName }}</li>
    </ul>
</div>
<div class="col">
</div>
</div>
</div>
`
}

const router = new VueRouter({
  routes: [
     { path: '/play/:story_id', component: Story,
       children: [
            { path: '', component: StoryInit },
            { path: '/play/:story_id/:player_id', component: StoryRun },
       ]
     },
     { path: '/', component: StoryCreate }
 ],

})

const app = new Vue({
  router
}).$mount('#app')
